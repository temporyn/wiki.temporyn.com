package com.temporyn.wiki.repository;

import com.temporyn.wiki.domain.Directory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
}
