package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Curso;
import com.CristoMorado_api.CristoMorado_api.util.CategoriaCurso;

/** Curso del catálogo. "secciones" = en cuántas secciones está asignado. */
public record CursoResponse(Long id, String nombre, String categoria, String categoriaNombre,
                            String color, String estado, long secciones) {

    public static CursoResponse fromEntity(Curso c) {
        return fromEntity(c, 0);
    }

    public static CursoResponse fromEntity(Curso c, long secciones) {
        CategoriaCurso.Info cat = CategoriaCurso.de(c.getCategoria());
        return new CursoResponse(c.getId(), c.getNombre(), c.getCategoria(),
                cat == null ? "Otros" : cat.nombre(), c.getColor(), c.getEstado(), secciones);
    }
}
