package com.andreavendrame.ldb4docker;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConfigurationRepository extends CrudRepository<UserConfiguration, Integer> {
}
