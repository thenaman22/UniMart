package com.unimart.api;

import com.unimart.api.dto.ListingDtos.CreateListingRequest;
import com.unimart.api.dto.ListingDtos.ListingMediaInput;
import com.unimart.api.dto.ListingDtos.ReportListingRequest;
import com.unimart.api.dto.ListingDtos.UpdateListingRequest;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.ListingStatus;
import com.unimart.domain.MediaType;
import com.unimart.domain.Report;
import com.unimart.repository.ReportRepository;
import com.unimart.service.ApiException;
import com.unimart.service.ListingService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final ListingService listingService;
    private final ReportRepository reportRepository;

    public ListingController(ListingService listingService, ReportRepository reportRepository) {
        this.listingService = listingService;
        this.reportRepository = reportRepository;
    }

    @GetMapping
    public List<Map<String, Object>> search(
        @CurrentUser AuthContext authContext,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String itemCondition,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "ACTIVE") ListingStatus status
    ) {
        requireAuth(authContext);
        return listingService.searchListings(authContext.user(), query, category, itemCondition, minPrice, maxPrice, status)
            .stream()
            .map(listing -> Mapper.listingSummary(listing, listingService.mediaForListing(listing.getId(), authContext.user())))
            .toList();
    }

    @GetMapping("/{listingId}")
    public Map<String, Object> detail(@PathVariable Long listingId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        Listing listing = listingService.getAccessibleListing(listingId, authContext.user());
        return Mapper.listingDetail(listing, listingService.mediaForListing(listingId, authContext.user()));
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateListingRequest request, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        Listing listing = listingService.createListing(
            authContext.user(),
            request.communityId(),
            request.title(),
            request.description(),
            request.price(),
            request.category(),
            request.itemCondition(),
            request.media().stream().map(this::toListingMedia).toList()
        );
        return Mapper.listingDetail(listing, listingService.mediaForListing(listing.getId(), authContext.user()));
    }

    @PatchMapping("/{listingId}/status")
    public Map<String, Object> updateStatus(
        @PathVariable Long listingId,
        @RequestParam ListingStatus status,
        @CurrentUser AuthContext authContext
    ) {
        requireAuth(authContext);
        Listing listing = listingService.updateStatus(listingId, authContext.user(), status);
        return Mapper.listingSummary(listing, listingService.mediaForListing(listing.getId(), authContext.user()));
    }

    @PatchMapping("/{listingId}")
    public Map<String, Object> update(
        @PathVariable Long listingId,
        @Valid @RequestBody UpdateListingRequest request,
        @CurrentUser AuthContext authContext
    ) {
        requireAuth(authContext);
        Listing listing = listingService.updateListing(
            listingId,
            authContext.user(),
            request.title(),
            request.description(),
            request.price(),
            request.category(),
            request.itemCondition()
        );
        return Mapper.listingSummary(listing, listingService.mediaForListing(listing.getId(), authContext.user()));
    }

    @DeleteMapping("/{listingId}")
    public void delete(@PathVariable Long listingId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        listingService.deleteListing(listingId, authContext.user());
    }

    @PostMapping("/{listingId}/reports")
    public Map<String, Object> report(
        @PathVariable Long listingId,
        @Valid @RequestBody ReportListingRequest request,
        @CurrentUser AuthContext authContext
    ) {
        requireAuth(authContext);
        Listing listing = listingService.getAccessibleListing(listingId, authContext.user());
        Report report = new Report();
        report.setListing(listing);
        report.setReporter(authContext.user());
        report.setReason(request.reason());
        report.setResolved(false);
        reportRepository.save(report);
        return Mapper.report(report);
    }

    private ListingMedia toListingMedia(ListingMediaInput input) {
        ListingMedia media = new ListingMedia();
        media.setStorageKey(input.storageKey());
        media.setContentType(input.contentType());
        media.setFileSize(input.fileSize());
        media.setType(input.contentType().startsWith("video/") ? MediaType.VIDEO : MediaType.IMAGE);
        return media;
    }

    private void requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
