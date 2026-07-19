package com.temporyn.wiki.repository;

import com.temporyn.wiki.domain.Directory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {

    List<Directory> findByParentId(Long parentId);

    List<Directory> findByParentIdIsNull();
}
