package com.andreavendrame.ldb4docker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service("userConfigurationService")
public class UserConfigurationServiceImpl implements UserConfigurationService {

    @Autowired
    private UserConfigurationRepository repository;

    @Override
    public void saveConfiguration(UserConfiguration configuration) {
        repository.save(configuration);
    }

    @Override
    public List<UserConfiguration> findAllConfigurations() {

        List<UserConfiguration> configurations = new LinkedList<>();

        for (UserConfiguration configuration : repository.findAll()) {
            configurations.add(configuration);
        }

        return configurations;
    }
}
