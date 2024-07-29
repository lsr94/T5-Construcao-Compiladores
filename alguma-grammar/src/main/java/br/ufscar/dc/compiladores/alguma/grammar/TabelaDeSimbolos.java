package br.ufscar.dc.compiladores.alguma.grammar;

import java.util.HashMap;
import java.util.Map;

public class TabelaDeSimbolos {
    public enum AlgumaGrammar {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        INVALIDO
    }

    class EntradaTabelaDeSimbolos {
        String nome;
        AlgumaGrammar tipo;

        private EntradaTabelaDeSimbolos(String nome, AlgumaGrammar tipo) {
            this.nome = nome;
            this.tipo = tipo;
        }
    }

    private final Map<String, EntradaTabelaDeSimbolos> tabela;

    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }
    
    public void adicionar(String nome, AlgumaGrammar tipo) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo));
    }
    
    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }
    
    public AlgumaGrammar verificar(String nome) {
        return tabela.get(nome).tipo;
    }

}
