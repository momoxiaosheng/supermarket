package com.example.supermarket2.service.impl;


import com.example.supermarket2.dto.app.response.BannerDto;
import com.example.supermarket2.entity.Banner;
import com.example.supermarket2.mapper.BannerMapper;
import com.example.supermarket2.service.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public List<BannerDto> getActiveBanners() {
        List<Banner> banners = bannerMapper.selectActiveBanners();
        return banners.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private BannerDto convertToDto(Banner banner) {
        BannerDto dto = new BannerDto();
        dto.setId(banner.getId());
        dto.setImage(banner.getImage());
        log.debug("促销活动[" + banner.getId() + "]的image解析结果: " + banner.getImage());
        dto.setUrl(banner.getUrl());
        return dto;
    }
}
