package com.andreavendrame.ldb4docker;

import org.springframework.stereotype.Service;

import java.util.List;

public interface UserConfigurationService {

    public void saveConfiguration(UserConfiguration configuration);

    public List<UserConfiguration> findAllConfigurations();
}
