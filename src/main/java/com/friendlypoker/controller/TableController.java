package com.friendlypoker.controller;

import com.friendlypoker.dto.CreateTableRequest;
import com.friendlypoker.dto.TableResponse;
import com.friendlypoker.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @PostMapping("/api/clubs/{clubId}/tables")
    public ResponseEntity<TableResponse> create(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateTableRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(tableService.createTable(clubId, req, user.getUsername()));
    }

    @GetMapping("/api/clubs/{clubId}/tables")
    public List<TableResponse> list(
            @PathVariable Long clubId,
            @AuthenticationPrincipal UserDetails user) {
        return tableService.getClubTables(clubId, user.getUsername());
    }

    @GetMapping("/api/tables/{id}")
    public TableResponse get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return tableService.getTable(id, user.getUsername());
    }

    @PostMapping("/api/tables/{id}/sit")
    public ResponseEntity<TableResponse> sit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(tableService.sitDown(id, user.getUsername()));
    }

    @DeleteMapping("/api/tables/{id}/sit")
    public ResponseEntity<Void> standUp(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        tableService.standUp(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
