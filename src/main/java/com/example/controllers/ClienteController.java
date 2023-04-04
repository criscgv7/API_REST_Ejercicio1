package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.entities.Cliente;
import com.example.model.FileDownloadUtil;
import com.example.model.FileUploadResponse;
import com.example.services.ClienteService;
import com.example.utilities.FileUploadUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

//1. BUSCA PAGINADA
@RequiredArgsConstructor
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private FileUploadUtil fileUploadUtil;
    private final FileDownloadUtil fileDownloadUtil; 

    

    @GetMapping
    public ResponseEntity<List<Cliente>> findAll(@RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        ResponseEntity<List<Cliente>> responseEntity = null;
        List<Cliente> clientes = new ArrayList<>();
        Sort sortByNombre = Sort.by("nombre");

        if (page != null && size != null) {
            try {
                Pageable pageable = PageRequest.of(page, size, sortByNombre);
                Page<Cliente> clientesPaginados = clienteService.findAll(pageable);
                clientes = clientesPaginados.getContent();
                responseEntity = new ResponseEntity<List<Cliente>>(clientes, HttpStatus.OK);
            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {
            try {
                clientes = clienteService.findAll(sortByNombre);
                responseEntity = new ResponseEntity<List<Cliente>>(clientes, HttpStatus.OK);
            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

        }

        return responseEntity;
    }

    // 2. BUSCA POR ID

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable(name = "id") Integer id) {
        ResponseEntity<Map<String, Object>> responseEntity = null;
        Map<String, Object> responseAsMap = new HashMap<>();

        try {
            Cliente cliente = clienteService.findById(id);
            if (cliente != null) {
                String successMessage = "Se ha encontrado el cliente con id: " + id + " correctamente";
                responseAsMap.put("mensaje", successMessage);
                responseAsMap.put("cliente", cliente);
                // responseAsMap.put("mascotas", cliente.getMascotas());
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);
            } else {
                String errorMessage = "No se ha encontrado el cliente con id: " + id;
                responseAsMap.put("error", errorMessage);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            String errorGrave = "Error grave";
            responseAsMap.put("error", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }

    // 3.GUARDAR UNA FICHA DE CLIENTE

    @PostMapping(consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<Map<String, Object>> insertaFicha(@Valid @RequestPart(name = "cliente") Cliente cliente,
            BindingResult result, @RequestPart(name = "file") MultipartFile file)
            throws IOException {
        Map<String, Object> responseAsMap = new HashMap<>();
        ResponseEntity<Map<String, Object>> responseEntity = null;

        // VEMOS SI HAY ERRORES

        if (result.hasErrors()) {
            List<String> errorMensajes = new ArrayList<>();
            for (ObjectError error : result.getAllErrors()) {
                errorMensajes.add(error.getDefaultMessage());
            }
            responseAsMap.put("errores", errorMensajes);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);
            return responseEntity;
        }

        // NO HAY ERRRORES, VAMOS A GUARDAR FICHA

        if (!file.isEmpty()) {
            String fileCode = fileUploadUtil.saveFile(file.getOriginalFilename(), file);
            cliente.setImagen(fileCode + file.getOriginalFilename());

            // CON INFORMACION

            FileUploadResponse fileUploadResponse = FileUploadResponse.builder()
                    .fileName(fileCode + file.getOriginalFilename()).downloadURI("/clientes/imagen/"
                            + fileCode + file.getOriginalFilename())
                    .size(file.getSize()).build();

            responseAsMap.put("info del archivo", fileUploadResponse);

        }
        Cliente clienteDB = clienteService.save(cliente);

        // COMPROBAMOS

        try {

            if (clienteDB != null) {
                String mensaje = "Se ha guardado la ficha de Cliente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("cliente", clienteDB);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.CREATED);
            } else {
                String errorMensaje = "No se ha podido registrar la ficha de Cliente";
                responseAsMap.put("errorMensaje", errorMensaje);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap,
                        HttpStatus.INTERNAL_SERVER_ERROR);

            }
        } catch (DataAccessException e) {
            String errorGrave = "Ha tenido lugar un error grave y la causa más probable "
                    + "puede ser " + e.getMostSpecificCause();
            responseAsMap.put("errorGrave", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }

    // UPDATE

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Cliente cliente,
            BindingResult result, @PathVariable(name = "id") Integer id) {

        Map<String, Object> responseAsMap = new HashMap<>();
        ResponseEntity<Map<String, Object>> responseEntity = null;

        // ERRORES

        if (result.hasErrors()) {
            List<String> errorMessages = new ArrayList<>();

            for (ObjectError error : result.getAllErrors()) {
                errorMessages.add(error.getDefaultMessage());
            }
            responseAsMap.put("errores", errorMessages);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);
            return responseEntity;
        }

   //NO ERRORES, ACTUALIZAMOS
        cliente.setId(id);
        Cliente clienteDB = clienteService.save(cliente);

        try {

            if (clienteDB != null) {
                String mensaje = "El cliente se ha actualizado correctamente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("cliente", clienteDB);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.CREATED);
            } else {
                // No se ha creado el producto
                String errorMensaje = "No se ha actualizado el cliente";
                responseAsMap.put("errorMensaje", errorMensaje);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap,
                        HttpStatus.INTERNAL_SERVER_ERROR);

            }
        } catch (DataAccessException e) {
            String errorGrave = "Ha tenido lugar un error grave"
                    + "y, la causa mas probable puede ser " + e.getMostSpecificCause();
            responseAsMap.put("errorGrave", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }


    //BORRAR
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> delete(@PathVariable(name = "id") Integer id) {
        ResponseEntity<String> responseEntity = null;

        try {
            //PROBAMOS QUE NO SEA NULL
            Cliente cliente= clienteService.findById(id);

            if (cliente != null) {
                clienteService.delete(cliente);
                responseEntity = new ResponseEntity<String>("Borrado exitosamente", HttpStatus.OK);
            } else {

                responseEntity = new ResponseEntity<String>("No existe el producto buscado", HttpStatus.NOT_FOUND);
            }

        } catch (DataAccessException e) {
            e.getMostSpecificCause();
            responseEntity = new ResponseEntity<String>("Error fatal", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;

    }

    //METODO PARA DESCARGAR LA FOTO


    /**
     *  Implementa filedownnload end point API 
     **/    
    @GetMapping("/downloadFile/{fileCode}")
    public ResponseEntity<?> downloadFile(@PathVariable(name = "fileCode") String fileCode) {

        Resource resource = null;

        try {
            resource = fileDownloadUtil.getFileAsResource(fileCode);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (resource == null) {
            return new ResponseEntity<>("File not found ", HttpStatus.NOT_FOUND);
        }

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
        .body(resource);

    }
}
