package unioeste.geral.endereco.col;

import unioeste.geral.endereco.bo.*;
import unioeste.geral.endereco.dao.*;
import java.sql.Connection;
import java.util.List;

public class CidadeCol {
    public boolean validarId(int id) {
        return id > 0;
    }

    public boolean validarCidadeParaConsulta(Cidade cidade) {
        return cidade != null && validarId(cidade.getIdCidade());
    }
}