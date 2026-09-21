package com.neobanco.backend.service;

import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GamificacionService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public void sumarXP(Usuario usuario, int puntos) {
        if (usuario == null) return;
        
        int nuevaXP = (usuario.getExperiencia() != null ? usuario.getExperiencia() : 0) + puntos;
        int nivelActual = usuario.getNivel() != null ? usuario.getNivel() : 1;
        
        int xpRequerida = nivelActual * 100;
        
        if (nuevaXP >= xpRequerida) {
            usuario.setNivel(nivelActual + 1);
            usuario.setExperiencia(nuevaXP - xpRequerida);
        } else {
            usuario.setExperiencia(nuevaXP);
        }
        
        usuarioRepository.save(usuario);
    }
}
