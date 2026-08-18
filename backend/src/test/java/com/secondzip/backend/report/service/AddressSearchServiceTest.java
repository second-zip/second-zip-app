package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AddressCandidate;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.response.AddressSearchResponse;
import com.secondzip.backend.report.service.external.client.AddressClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressSearchServiceTest {

    @Mock
    private AddressClient addressClient;

    @Mock
    private AddressSearchStore addressSearchStore;

    private AddressSearchService service;

    @BeforeEach
    void setUp() {
        service = new AddressSearchService(addressClient, addressSearchStore);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("검색어가 없거나 공백이면 외부 주소 API를 호출하지 않는다")
    void rejectsBlankQuery(String query) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.search(query)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(exception).hasMessage("검색어를 입력해주세요.");
        verifyNoInteractions(addressClient, addressSearchStore);
    }

    @Test
    @DisplayName("검색어를 trim하고 각 후보의 내부 식별값은 저장소에 보관한 뒤 화면 필드만 반환한다")
    void storesCandidatesAndReturnsPublicAddressFields() {
        AnalysisTarget firstTarget = target(
                "서울특별시 강남구 테헤란로 1",
                "서울특별시 강남구 역삼동 1"
        );
        AnalysisTarget secondTarget = target(
                "서울특별시 강남구 테헤란로 2",
                "서울특별시 강남구 역삼동 2"
        );
        AddressCandidate first = new AddressCandidate(
                firstTarget,
                "06236",
                "첫 번째 건물"
        );
        AddressCandidate second = new AddressCandidate(
                secondTarget,
                null,
                null
        );
        when(addressClient.search("테헤란로"))
                .thenReturn(List.of(first, second));
        when(addressSearchStore.save(firstTarget)).thenReturn("address-1");
        when(addressSearchStore.save(secondTarget)).thenReturn("address-2");

        AddressSearchResponse response = service.search("  테헤란로  ");

        assertThat(response.getAddresses()).hasSize(2);
        assertThat(response.getAddresses().get(0).getAddressId())
                .isEqualTo("address-1");
        assertThat(response.getAddresses().get(0).getRoadAddress())
                .isEqualTo(firstTarget.roadAddress());
        assertThat(response.getAddresses().get(0).getJibunAddress())
                .isEqualTo(firstTarget.lotAddress());
        assertThat(response.getAddresses().get(0).getZoneNo())
                .isEqualTo("06236");
        assertThat(response.getAddresses().get(0).getPlaceName())
                .isEqualTo("첫 번째 건물");
        assertThat(response.getAddresses().get(1).getAddressId())
                .isEqualTo("address-2");
        verify(addressClient).search("테헤란로");
        verify(addressSearchStore).save(firstTarget);
        verify(addressSearchStore).save(secondTarget);
    }

    @Test
    @DisplayName("검색 후보가 없으면 저장 없이 빈 배열을 반환한다")
    void returnsEmptyResponseForNoCandidates() {
        when(addressClient.search("없는 주소")).thenReturn(List.of());

        AddressSearchResponse response = service.search("없는 주소");

        assertThat(response.getAddresses()).isEmpty();
        verify(addressSearchStore, never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("외부 주소 검색 실패를 숨기지 않고 후보 저장 전에 전파한다")
    void propagatesAddressClientFailure() {
        BusinessException failure = new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                "주소 검색 실패"
        );
        when(addressClient.search("테헤란로")).thenThrow(failure);

        assertThatThrownBy(() -> service.search("테헤란로"))
                .isSameAs(failure);
        verifyNoInteractions(addressSearchStore);
    }

    private AnalysisTarget target(String roadAddress, String lotAddress) {
        return new AnalysisTarget(
                roadAddress,
                roadAddress,
                "1168010100",
                "11680",
                "10100",
                "1",
                "0",
                "1",
                "0",
                "building-management-no",
                "역삼동",
                lotAddress
        );
    }
}
