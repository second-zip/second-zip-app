param(
    [string]$BaseUrl = "http://localhost:8080/secondzip"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

function ConvertFrom-SecureInput {
    param([Security.SecureString]$SecureValue)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR(
        $SecureValue
    )
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Invoke-SecondZipApi {
    param(
        [ValidateSet("GET", "POST")]
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [hashtable]$Headers
    )

    $arguments = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $Headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $arguments.Body = $Body | ConvertTo-Json -Depth 10
    }
    try {
        return Invoke-RestMethod @arguments
    }
    catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            Write-Host "HTTP 오류: $([int]$response.StatusCode)" -ForegroundColor Red
        }
        throw
    }
}

function Show-CaptchaImage {
    param(
        [string]$ImageData,
        [string]$RequestId
    )

    if ([string]::IsNullOrWhiteSpace($ImageData)) {
        throw "CAPTCHA 단계이지만 CODEF 보안문자 이미지가 비어 있습니다."
    }

    $base64 = $ImageData
    if ($base64 -match "^data:image/[^;]+;base64,(.+)$") {
        $base64 = $Matches[1]
    }
    $captchaPath = Join-Path `
        ([IO.Path]::GetTempPath()) `
        "secondzip-captcha-$RequestId.png"
    [IO.File]::WriteAllBytes(
        $captchaPath,
        [Convert]::FromBase64String($base64)
    )
    Start-Process -FilePath $captchaPath
    return $captchaPath
}

Write-Host "SECONDZIP CODEF 데모 E2E" -ForegroundColor Cyan
Write-Host "서버: $BaseUrl"

$email = Read-Host "SECONDZIP 로그인 이메일"
$password = ConvertFrom-SecureInput (
    Read-Host "SECONDZIP 로그인 비밀번호" -AsSecureString
)
$login = Invoke-SecondZipApi -Method POST -Path "/api/auth/login" -Body @{
    email = $email
    password = $password
} -Headers @{}
$password = $null

$headers = @{
    Authorization = "Bearer $($login.accessToken)"
}

$readiness = Invoke-SecondZipApi `
    -Method GET `
    -Path "/api/analysis-reports/external-readiness" `
    -Body $null `
    -Headers $headers
if (-not $readiness.ready) {
    Write-Host "외부 API 설정이 준비되지 않았습니다." -ForegroundColor Red
    Write-Host "누락: $($readiness.missingConfigurations -join ', ')"
    Write-Host "경고: $($readiness.warnings -join ', ')"
    exit 1
}
Write-Host "외부 API 준비상태: 정상 ($($readiness.codefEnvironment))" -ForegroundColor Green

$roadAddress = Read-Host "도로명주소 [서울특별시 송파구 송파대로 345]"
if ([string]::IsNullOrWhiteSpace($roadAddress)) {
    $roadAddress = "서울특별시 송파구 송파대로 345"
}
$detailAddress = Read-Host "상세주소 [101동 1304호]"
if ([string]::IsNullOrWhiteSpace($detailAddress)) {
    $detailAddress = "101동 1304호"
}
$depositInput = Read-Host "보증금(원) [500000000]"
if ([string]::IsNullOrWhiteSpace($depositInput)) {
    $depositInput = "500000000"
}
$deposit = [long]$depositInput

$userName = Read-Host "간편인증 실제 이름"
$birthDate = Read-Host "생년월일 8자리(yyyyMMdd)"
$phoneNo = Read-Host "휴대폰번호(하이픈 없이)"
$provider = Read-Host "인증수단 [KAKAO/PASS/NAVER/TOSS] (기본 KAKAO)"
if ([string]::IsNullOrWhiteSpace($provider)) {
    $provider = "KAKAO"
}
$provider = $provider.ToUpperInvariant()
$telecom = $null
if ($provider -eq "PASS") {
    $telecom = (Read-Host "통신사 [SKT/KT/LG_U_PLUS]").ToUpperInvariant()
}

$authentication = @{
    userName = $userName
    birthDate = $birthDate
    phoneNo = $phoneNo
    provider = $provider
    telecom = $telecom
    consent = $true
}

$workflow = Invoke-SecondZipApi `
    -Method POST `
    -Path "/api/analysis-reports/requests" `
    -Body @{
        roadAddress = $roadAddress
        detailAddress = $detailAddress
        deposit = $deposit
    } `
    -Headers $headers

$requestId = $workflow.requestId
Write-Host "분석 요청 생성: $requestId" -ForegroundColor Green
Write-Host "건물 유형: $($workflow.buildingType)"
Write-Host "필요 문서: $($workflow.requiredDocuments -join ', ')"

$state = $workflow
for ($step = 1; $step -le 30; $step++) {
    Write-Host "[$step] 상태: $($state.status), 다음 작업: $($state.nextAction)"

    switch ($state.status) {
        "AUTH_REQUIRED" {
            $state = Invoke-SecondZipApi `
                -Method POST `
                -Path "/api/analysis-reports/requests/$requestId/auth/start" `
                -Body $authentication `
                -Headers $headers
        }
        "AUTH_PENDING" {
            Write-Host "휴대폰에서 $provider 간편인증을 승인하세요." -ForegroundColor Yellow
            Read-Host "승인을 완료한 뒤 Enter"
            $state = Invoke-SecondZipApi `
                -Method POST `
                -Path "/api/analysis-reports/requests/$requestId/auth/continue" `
                -Body @{
                    authentication = $authentication
                    selectionValue = $null
                    secureNo = $null
                } `
                -Headers $headers
        }
        "SELECTION_REQUIRED" {
            $selectionValue = $null
            $secureNo = $null
            if ($state.nextAction -eq "CAPTCHA") {
                $captchaPath = Show-CaptchaImage `
                    -ImageData $state.captchaImage `
                    -RequestId $requestId
                $secureNo = Read-Host "화면에 표시된 보안문자"
                Remove-Item -LiteralPath $captchaPath -Force `
                    -ErrorAction SilentlyContinue
            }
            else {
                if ($null -eq $state.selectionOptions -or
                    $state.selectionOptions.Count -eq 0) {
                    throw "선택 단계인데 selectionOptions가 비어 있습니다."
                }
                for ($index = 0; $index -lt $state.selectionOptions.Count; $index++) {
                    Write-Host "$($index + 1). $($state.selectionOptions[$index].label)"
                }
                $selectedIndex = [int](Read-Host "선택 번호") - 1
                if ($selectedIndex -lt 0 -or
                    $selectedIndex -ge $state.selectionOptions.Count) {
                    throw "선택 번호가 범위를 벗어났습니다."
                }
                $selectionValue =
                    $state.selectionOptions[$selectedIndex].value
            }
            $state = Invoke-SecondZipApi `
                -Method POST `
                -Path "/api/analysis-reports/requests/$requestId/auth/continue" `
                -Body @{
                    authentication = $authentication
                    selectionValue = $selectionValue
                    secureNo = $secureNo
                } `
                -Headers $headers
        }
        "PROCESSING" {
            $report = Invoke-SecondZipApi `
                -Method POST `
                -Path "/api/analysis-reports/requests/$requestId/complete" `
                -Body $null `
                -Headers $headers
            Write-Host "E2E 성공" -ForegroundColor Green
            Write-Host "리포트 ID: $($report.analysisReportId)"
            Write-Host "최종 위험도: $($report.result)"
            Write-Host "주소: $($report.roadAddress) $($report.detailAddress)"
            $illegalBuilding = $report.checkResults |
                Where-Object { $_.checkType -eq "ILLEGAL_BUILDING" } |
                Select-Object -First 1
            if ($null -ne $illegalBuilding) {
                Write-Host "위반건축물: $($illegalBuilding.evidence.isIllegalBuilding)"
                Write-Host "위반건축물 검증: $($illegalBuilding.evidence.isIllegalBuildingVerified)"
                Write-Host "판정 출처: $($illegalBuilding.evidence.source)"
                Write-Host "문서별 판정: $(
                    $illegalBuilding.evidence.violationByDocument |
                        ConvertTo-Json -Compress
                )"
            }
            exit 0
        }
        "FAILED" {
            Write-Host "최종 분석 실패: $($state.failureMessage)" -ForegroundColor Red
            $retry = Read-Host "인증을 반복하지 않고 재시도할까요? [y/N]"
            if ($retry.ToLowerInvariant() -ne "y") {
                exit 1
            }
            $report = Invoke-SecondZipApi `
                -Method POST `
                -Path "/api/analysis-reports/requests/$requestId/retry" `
                -Body $null `
                -Headers $headers
            Write-Host "재시도 성공, 리포트 ID: $($report.analysisReportId)" -ForegroundColor Green
            exit 0
        }
        "COMPLETED" {
            Write-Host "이미 완료된 요청입니다. 리포트 ID: $($state.reportId)" -ForegroundColor Green
            exit 0
        }
        default {
            throw "처리할 수 없는 상태입니다: $($state.status)"
        }
    }
}

throw "30단계 안에 E2E 흐름이 끝나지 않았습니다."
