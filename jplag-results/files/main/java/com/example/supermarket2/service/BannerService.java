package com.example.supermarket2.service;


import com.example.supermarket2.dto.app.BannerDto;

import java.util.List;

public interface BannerService {
    List<BannerDto> getActiveBanners();
}
