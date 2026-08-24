package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AddressCandidateDTO;
import com.secondzip.backend.report.dto.response.AddressSearchItem;
import com.secondzip.backend.report.dto.response.AddressSearchResponse;
import com.secondzip.backend.report.service.external.client.AddressClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressSearchService {

    private final AddressClient addressClient;
    private final AddressSearchStore addressSearchStore;

    public AddressSearchResponse search(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "검색어를 입력해주세요."
            );
        }

        List<AddressCandidateDTO> candidates = addressClient.search(query.trim());

        // 후보마다 식별값을 보관하고 addressId를 발급한다.
        // 응답에는 법정동코드·본번 같은 내부 값을 담지 않는다. 프론트가 알 필요가 없고,
        // 클라이언트가 보낸 값을 그대로 믿는 상황도 만들지 않기 위해서다.
        List<AddressSearchItem> items = candidates.stream()
                .map(candidate -> new AddressSearchItem(
                        addressSearchStore.save(candidate.target()),
                        candidate.target().roadAddress(),
                        candidate.target().lotAddress(),
                        candidate.zoneNo(),
                        candidate.placeName()
                ))
                .collect(Collectors.toList());

        return new AddressSearchResponse(items);
    }
}
