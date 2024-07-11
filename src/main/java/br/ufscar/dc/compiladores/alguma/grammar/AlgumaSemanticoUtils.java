package br.ufscar.dc.compiladores.alguma.grammar;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.AlgumaGrammar;

public class AlgumaSemanticoUtils {
    // Lista que armazenará os erros semânticos
    public static List<String> errosSemanticos = new ArrayList<>();
    
    // Método auxiliar para adicionar os erros identificados
    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        int coluna = t.getCharPositionInLine();
        errosSemanticos.add(String.format("Erro %d:%d - %s", linha, coluna, mensagem));
    }
    
    /*Método auxiliar que verifica a compatibilidade entre operadores aritméticos
    Caso a operação envolva pelo menos um valor real, a operação deve ser tratada
    como uma operação entre números reais, mesmo que um deles seja um inteiro */
    public static boolean verificaCompatibilidade(AlgumaGrammar T1, AlgumaGrammar T2) {
        boolean flag = false;
        
        if (T1 == AlgumaGrammar.INTEIRO && T2 == AlgumaGrammar.REAL)
            flag = true;
        else if (T1 == AlgumaGrammar.REAL && T2 == AlgumaGrammar.INTEIRO)
            flag = true;
        else if (T1 == AlgumaGrammar.REAL && T2 == AlgumaGrammar.REAL)
            flag = true;
        
        return flag;
    }
    
    // Método auxiliar que verifica a compatibilidade entre operadores para tratá-los como uma operação lógica
    public static boolean verificaCompatibilidadeLogica(AlgumaGrammar T1, AlgumaGrammar T2) {
        boolean flag = false;
        
        if (T1 == AlgumaGrammar.INTEIRO && T2 == AlgumaGrammar.REAL)
            flag = true;
        else if (T1 == AlgumaGrammar.REAL && T2 == AlgumaGrammar.INTEIRO)
            flag = true;

        return flag;
    }

    // Verifica os tipos das Expressões Aritméticas
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Exp_aritmeticaContext ctx) {
        AlgumaGrammar ret = null;
        for (var ta : ctx.termo()) {
            AlgumaGrammar aux = verificarTipo(tabela, ta);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != AlgumaGrammar.INVALIDO) {
                adicionarErroSemantico(ctx.start, "Expressão " + ctx.getText() + " contém tipos incompatíveis");
                ret = AlgumaGrammar.INVALIDO;
            }
        }

        return ret;
    }

    // Verifica os tipos dos Termos Aritméticos
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.TermoContext ctx) {
        AlgumaGrammar ret = null;

        for (var fa : ctx.fator()) {
            AlgumaGrammar aux = verificarTipo(tabela, fa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != AlgumaGrammar.INVALIDO) {
                adicionarErroSemantico(ctx.start, "Termo " + ctx.getText() + " contém tipos incompatíveis");
                ret = AlgumaGrammar.INVALIDO;
            }
        }
        return ret;
    }

    // Verifica os tipos dos Fatores Aritméticos
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.FatorContext ctx) {
        if (ctx.NUM_INT() != null) {
            return AlgumaGrammar.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return AlgumaGrammar.REAL;
        }
        if (ctx.VARIAVEL() != null) {
            String nomeVar = ctx.VARIAVEL().getText();
            if (!tabela.existe(nomeVar)) {
                adicionarErroSemantico(ctx.VARIAVEL().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
                return AlgumaGrammar.INVALIDO;
            }
            return verificarTipo(tabela, nomeVar);
        }
        // se não for nenhum dos tipos acima, só pode ser uma expressão entre parêntesis
        return verificarTipo(tabela, ctx.Exp_aritmeticaContext());
    }
    

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, String nomeVar) {
        return tabela.verificar(nomeVar);
    }
}
