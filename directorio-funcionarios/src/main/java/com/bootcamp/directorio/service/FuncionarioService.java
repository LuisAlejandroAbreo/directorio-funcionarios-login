package com.bootcamp.directorio.service;

import com.bootcamp.directorio.model.Funcionario;
import com.bootcamp.directorio.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;

    public FuncionarioService(FuncionarioRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista según los filtros. Los filtros vacíos o solo con espacios se tratan como ausentes.
     * Si llegan ambos con valor, se aplica el de nombre.
     */
    public List<Funcionario> listar(String nombre, String departamento) {
        if (tieneValor(nombre)) {
            return repository.findByNombreContainingIgnoreCase(nombre.trim());
        }
        if (tieneValor(departamento)) {
            return repository.findByDepartamentoIgnoreCase(departamento.trim());
        }
        return repository.findAll();
    }

    public Optional<Funcionario> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Funcionario crear(Funcionario funcionario) {
        funcionario.setId(null);
        normalizar(funcionario);
        return repository.save(funcionario);
    }

    public Optional<Funcionario> actualizar(Long id, Funcionario datos) {
        return repository.findById(id).map(existente -> {
            existente.setNombre(datos.getNombre());
            existente.setCargo(datos.getCargo());
            existente.setDepartamento(datos.getDepartamento());
            existente.setTelefono(datos.getTelefono());
            normalizar(existente);
            return repository.save(existente);
        });
    }

    public boolean eliminar(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private boolean tieneValor(String texto) {
        return texto != null && !texto.isBlank();
    }

    private void normalizar(Funcionario f) {
        f.setNombre(f.getNombre().trim());
        f.setCargo(f.getCargo().trim());
        f.setDepartamento(f.getDepartamento().trim());
        if (f.getTelefono() != null) {
            String tel = f.getTelefono().trim();
            f.setTelefono(tel.isEmpty() ? null : tel);
        }
    }
}
