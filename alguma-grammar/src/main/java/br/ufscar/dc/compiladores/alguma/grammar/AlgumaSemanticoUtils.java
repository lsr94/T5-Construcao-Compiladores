package br.ufscar.dc.compiladores.alguma.grammar;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.AlgumaGrammar;

public class AlgumaSemanticoUtils {
    // Lista que armazenará os erros semânticos
    public static List<String> errosSemanticos = new ArrayList<>();
    
    // Método auxiliar para adicionar os erros identificados
    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        int coluna = t.getCharPositionInLine();
        errosSemanticos.add(String.format("Erro na linha %d:%d - %s", linha, coluna, mensagem));
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
    
    // Método auxiliar que verifica a compatibilidade entre operadores aritméticos para tratá-los como uma operação lógica
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
        AlgumaGrammar retorno = null;
        for (var ta : ctx.termo()) {
            AlgumaGrammar aux = verificarTipo(tabela, ta);
            if (retorno == null) {
                retorno = aux;
            } else if (retorno != aux && aux != AlgumaGrammar.INVALIDO) {
                adicionarErroSemantico(ctx.start, "Expressão " + ctx.getText() + " contém tipos incompatíveis");
                retorno = AlgumaGrammar.INVALIDO;
            }
        }

        return retorno;
    }

    // Verifica os tipos dos Termos Aritméticos
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.TermoContext ctx) {
        AlgumaGrammar retorno = null;

        for (var fa : ctx.fator()) {
            AlgumaGrammar aux = verificarTipo(tabela, fa);
            if (retorno == null) {
                retorno = aux;
            } else if (retorno != aux && aux != AlgumaGrammar.INVALIDO) {
                adicionarErroSemantico(ctx.start, "Termo " + ctx.getText() + " contém tipos incompatíveis");
                retorno = AlgumaGrammar.INVALIDO;
            }
        }
        return retorno;
    }

    // Verifica os tipos dos Fatores Aritméticos
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.FatorContext ctx) {
        AlgumaGrammar retorno = null;

        for (var parcela : ctx.parcela())
            retorno = verificarTipo(tabela, parcela);
        
        return retorno;
    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.ParcelaContext ctx) {
        // Identifica se é uma parcela unária ou não unária
        if (ctx.parcela_unario() != null)
            return verificarTipo(tabela, ctx.parcela_unario());
        else
            return verificarTipo(tabela, ctx.parcela_nao_unario());
    }
    
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Parcela_unarioContext ctx) {
        AlgumaGrammar retorno;
        String nome;
        
        // Verificação de variáveis (IDENT - identificador)
        if (ctx.identificador() != null) {
            nome = ctx.identificador().getText();
            
            // Se o identificador (variável) já tiver sido declarado, apenas retorna seu tipo
            if (tabela.existe(nome))
                retorno = tabela.verificar(nome);
            /* Se não tiver sido declarado: utiliza uma tabela auxiliar para realizar a verificação
            Se não tiver sido declarado, utiliza o método adicionaErroSemantico para verificar se o erro já foi
            exibido e, caso ainda não tenha sido, adiciona-o à lista */ 
            else {
                TabelaDeSimbolos tabelaAux = AlgumaSemantico.escoposAninhados.percorrerEscoposAninhados().get(AlgumaSemantico.escoposAninhados.percorrerEscoposAninhados().size() - 1);
                if (!tabelaAux.existe(nome)) {
                    adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                    retorno = AlgumaGrammar.INVALIDO;
                } else 
                    retorno = tabelaAux.verificar(nome);
            }
        } else if (ctx.NUM_INT() != null) // Verificação de números inteiros (NUM_INT - inteiro)
            retorno = AlgumaGrammar.INTEIRO;
        else if (ctx.NUM_REAL() != null) // Verificação de números reais (NUM_REAL - real)
            retorno = AlgumaGrammar.REAL;
        else
            retorno = verificarTipo(tabela, ctx.expressao().get(0));

        return retorno;
    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Parcela_nao_unarioContext ctx) {
        AlgumaGrammar retorno;
        String nome;

        if (ctx.identificador() != null) {
            nome = ctx.identificador().getText();
        
            if (!tabela.existe(nome)) {
                adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                retorno = AlgumaGrammar.INVALIDO;
            } else 
                retorno = tabela.verificar(ctx.identificador().getText());
        } else
            retorno = AlgumaGrammar.LITERAL;

        return retorno;
    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.ExpressaoContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.termo_logico(0));

        // Para expressões lógicas, a ideia resume-se apenas em verificar se os tipos analisados são diferentes
        for (AlgumaGrammarParser.Termo_logicoContext termoLogico : ctx.termo_logico()) {
            AlgumaGrammar tipoAtual = verificarTipo(tabela, termoLogico);
            if (retorno != tipoAtual && tipoAtual != AlgumaGrammar.INVALIDO)
                retorno = AlgumaGrammar.INVALIDO;
        }

        return retorno;
    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Termo_logicoContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.fator_logico(0));

        for (AlgumaGrammarParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
            AlgumaGrammar tipoAtual = verificarTipo(tabela, fatorLogico);
            if (retorno != tipoAtual && tipoAtual != AlgumaGrammar.INVALIDO)
                retorno = AlgumaGrammar.INVALIDO;
        }
        return retorno;
    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Fator_logicoContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.parcela_logica());
        return retorno;

    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Parcela_logicaContext ctx) {
        AlgumaGrammar retorno;

        if (ctx.exp_relacional() != null)
            retorno = verificarTipo(tabela, ctx.exp_relacional());
         else
            retorno = AlgumaGrammar.BOOL;

        return retorno;

    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Exp_relacionalContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.exp_aritmetica().get(0));

        if (ctx.exp_aritmetica().size() > 1) {
            AlgumaGrammar tipoAtual = verificarTipo(tabela, ctx.exp_aritmetica().get(1));

            // Semelhante ao que foi feito com as expressões aritméticas, ocorre uma verificação
            // para saber se a expressão atual pode ser tratada como uma operação lógica.
            if (retorno == tipoAtual || verificaCompatibilidadeLogica(retorno, tipoAtual))
                retorno = AlgumaGrammar.BOOL;
            else
                retorno = AlgumaGrammar.INVALIDO;
        }

        return retorno;

    }

    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, String nomeVar) {
        return tabela.verificar(nomeVar);
    }
}
