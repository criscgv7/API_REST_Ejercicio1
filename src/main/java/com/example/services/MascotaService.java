package com.example.services;

import java.util.List;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.entities.Mascota;


public interface MascotaService {
    public List<Mascota> findAll(Sort sort);
    public Page<Mascota> findAll(Pageable pageable);
    
}
