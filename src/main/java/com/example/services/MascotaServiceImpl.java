package com.example.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.dao.MascotaDao;
import com.example.entities.Cliente;
import com.example.entities.Mascota;

@Service
public class MascotaServiceImpl implements MascotaService {

    @Autowired
    private MascotaDao mascotaDao;

    @Override
    public List<Mascota> findAll(Sort sort) {
       return mascotaDao.findAll(); }

    @Override
    public Page<Mascota> findAll(Pageable pageable) {
        return mascotaDao.findAll(pageable); 
    }

    

}
