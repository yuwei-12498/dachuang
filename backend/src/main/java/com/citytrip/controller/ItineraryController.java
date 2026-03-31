package com.citytrip.controller;

import com.citytrip.annotation.LoginRequired;
import com.citytrip.common.AuthConstants;
import com.citytrip.model.dto.FavoriteReqDTO;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.ReplaceReqDTO;
import com.citytrip.model.dto.ReplanReqDTO;
import com.citytrip.model.dto.ReplanRespDTO;
import com.citytrip.model.vo.ItinerarySummaryVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.ItineraryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @LoginRequired
    @PostMapping
    public ResponseEntity<ItineraryVO> createItinerary(@RequestBody GenerateReqDTO req, HttpSession session) {
        ItineraryVO vo = itineraryService.generateUserItinerary(currentUserId(session), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @LoginRequired
    @GetMapping
    public List<ItinerarySummaryVO> listItineraries(@RequestParam(value = "favorite", defaultValue = "false") boolean favorite,
                                                    @RequestParam(value = "limit", required = false) Integer limit,
                                                    HttpSession session) {
        return itineraryService.listItineraries(currentUserId(session), favorite, limit);
    }

    @LoginRequired
    @GetMapping("/{id}")
    public ItineraryVO getItinerary(@PathVariable("id") Long id, HttpSession session) {
        return itineraryService.getItinerary(currentUserId(session), id);
    }

    @LoginRequired
    @PatchMapping("/{id}/replan")
    public ReplanRespDTO replanItinerary(@PathVariable("id") Long id,
                                         @RequestBody ReplanReqDTO req,
                                         HttpSession session) {
        return itineraryService.replan(currentUserId(session), id, req);
    }

    @LoginRequired
    @PatchMapping("/{id}/nodes/{poiId}/replacement")
    public ItineraryVO replacePoi(@PathVariable("id") Long id,
                                  @PathVariable("poiId") Long poiId,
                                  @RequestBody ReplaceReqDTO req,
                                  HttpSession session) {
        return itineraryService.replaceNode(currentUserId(session), id, poiId, req);
    }

    @LoginRequired
    @PutMapping("/{id}/favorite")
    public ItineraryVO favoriteItinerary(@PathVariable("id") Long id,
                                         @RequestBody(required = false) FavoriteReqDTO req,
                                         HttpSession session) {
        return itineraryService.favoriteItinerary(currentUserId(session), id, req);
    }

    @LoginRequired
    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> unfavoriteItinerary(@PathVariable("id") Long id, HttpSession session) {
        itineraryService.unfavoriteItinerary(currentUserId(session), id);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(HttpSession session) {
        return session == null ? null : (Long) session.getAttribute(AuthConstants.LOGIN_USER_ID);
    }
}
