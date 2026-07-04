package com.shortlink.controller;

import com.shortlink.common.Result;
import com.shortlink.dto.GenerateLinkRequest;
import com.shortlink.dto.LinkInfoResponse;
import com.shortlink.dto.LinkResponse;
import com.shortlink.service.LinkService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;

@RestController
public class LinkController {
    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/api/link/generate")
    public Result<LinkResponse> generate(@RequestBody GenerateLinkRequest request, HttpServletRequest servletRequest) {
        LinkResponse response = linkService.generate(request, baseUrl(servletRequest));
        return Result.success(response);
    }

    @GetMapping("/api/link/info/{code}")
    public Result<LinkInfoResponse> info(@PathVariable("code") String code) {
        return Result.success(linkService.getInfo(code));
    }

    @GetMapping("/api/link/recent")
    public Result<List<LinkInfoResponse>> recent() {
        return Result.success(linkService.listRecent(6));
    }

    @DeleteMapping("/api/link/{code}")
    public Result<Void> delete(@PathVariable("code") String code) {
        linkService.delete(code);
        return Result.success(null);
    }

    @GetMapping("/{code:[0-9a-zA-Z]{1,8}}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        String originalUrl = linkService.resolveOriginalUrl(code);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @RequestMapping("/api/health")
    public Result<String> health() {
        return Result.success("ok");
    }

    private String baseUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedProto != null && forwardedHost != null) {
            return forwardedProto + "://" + forwardedHost;
        }
        StringBuilder base = new StringBuilder();
        base.append(request.getScheme()).append("://").append(request.getServerName());
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            base.append(":").append(request.getServerPort());
        }
        return base.toString();
    }
}
