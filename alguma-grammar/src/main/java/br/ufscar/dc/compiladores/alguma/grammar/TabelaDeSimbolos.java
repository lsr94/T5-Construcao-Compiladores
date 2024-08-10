package br.ufscar.dc.compiladores.alguma.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabelaDeSimbolos {
    public enum AlgumaGrammar {
        INTEIRO,
        REAL,
        LITERAL,
        VOID,
        REGISTRO,
        LOGICO,
        INVALIDO,
    }

    public enum TipoEntrada {
        VARIAVEL, 
        PROCEDIMENTO,
        FUNCAO,
        BOOL
    }

    class EntradaTabelaDeSimbolos {
        String nome;
        AlgumaGrammar tipo;
        TipoEntrada tipoEnt;
        boolean flag_ponteiro = false;

        private EntradaTabelaDeSimbolos(String nome, AlgumaGrammar tipo, TipoEntrada tipoEnt, boolean flag_ponteiro) {
            this.nome = nome;
            this.tipo = tipo;
            this.tipoEnt = tipoEnt;
            this.flag_ponteiro = flag_ponteiro;
        }
    }

    private final Map<String, EntradaTabelaDeSimbolos> tabela;

    static HashMap<String, ArrayList<AlgumaGrammar>> tabelaFuncaoProcedimento = new HashMap<>();

    
    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }
    
    public void adicionar(String nome, AlgumaGrammar tipo, TipoEntrada tipoEnt, boolean flag_ponteir) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, tipoEnt, flag_ponteir));
    }
    
    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }
    
    public AlgumaGrammar verificar(String nome) {
        return tabela.get(nome).tipo;
    }

    public TipoEntrada verificartipoEnt(String nome) {
        return tabela.get(nome).tipoEnt;
    }

    public boolean verificar_ponteiro(String nome) {
        return tabela.get(nome).flag_ponteiro;
    }

    
}
