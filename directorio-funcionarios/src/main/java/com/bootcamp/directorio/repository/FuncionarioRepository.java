package com.bootcamp.directorio.repository;

import com.bootcamp.directorio.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    // Coincidencia parcial, sin distinguir mayúsculas/minúsculas
    List<Funcionario> findByNombreContainingIgnoreCase(String nombre);

    // Nombre completo del área, sin distinguir mayúsculas/minúsculas
    List<Funcionario> findByDepartamentoIgnoreCase(String departamento);
}
