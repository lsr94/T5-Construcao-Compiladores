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
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    /*
     * Método auxiliar que verifica a compatibilidade entre operadores aritméticos
     * Caso a operação envolva pelo menos um valor real, a operação deve ser tratada
     * como uma operação entre números reais, mesmo que um deles seja um inteiro
     */
    // public static boolean verificaCompatibilidade(AlgumaGrammar T1, AlgumaGrammar
    // T2, boolean flag_ponteiro_t1, boolean flag_ponteiro_t2) {
    // boolean flag = false;
    // if (flag_ponteiro_t1 == flag_ponteiro_t2){
    // flag = true;
    // if (T1==T2)
    // flag = true;
    // else if ((T1 == AlgumaGrammar.INTEIRO && T2 == AlgumaGrammar.REAL)||(T1 ==
    // AlgumaGrammar.REAL && T2 == AlgumaGrammar.INTEIRO))
    // flag = true;
    // else
    // flag = false;
    // } else
    // flag = false;
    // return flag;
    // }

    public static boolean verificaCompatibilidade(AlgumaGrammar T1, AlgumaGrammar T2) {
        boolean flag = false;

        if (T1 == T2)
            flag = true;
        else if ((T1 == AlgumaGrammar.INTEIRO && T2 == AlgumaGrammar.REAL) || (T1 == AlgumaGrammar.REAL && T2 == AlgumaGrammar.INTEIRO))
            flag = true;
        return flag;
    }

    // ############## ACHO QUE PODE APAGAR, POIS É IGUAL O DE CIMA ##############
    // Método auxiliar que verifica a compatibilidade entre operadores aritméticos
    // para tratá-los como uma operação lógica
    /*public static boolean verificaCompatibilidadeLogica(AlgumaGrammar T1, AlgumaGrammar T2) {
        boolean flag = false;

        if (T1 == AlgumaGrammar.INTEIRO && T2 == AlgumaGrammar.REAL)
            flag = true;
        else if (T1 == AlgumaGrammar.REAL && T2 == AlgumaGrammar.INTEIRO)
            flag = true;

        return flag;
    }*/

    // Verifica os tipos das Expressões Aritméticas
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Exp_aritmeticaContext ctx) {
        AlgumaGrammar retorno = null;
        for (var ta : ctx.termo()) {

            AlgumaGrammar aux = verificarTipo(tabela, ta);

            if ((verificaCompatibilidade(aux, retorno)) && (aux != AlgumaGrammar.INVALIDO))
                retorno = AlgumaGrammar.REAL;
            else
                retorno = aux;

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

                // Verifica se a variável de retorno já não é igual à auxiliar e se a variável
                // auxiliar não é inválida
            } else if (retorno != aux && aux != AlgumaGrammar.INVALIDO) {
                // Se uma das variáveis tem tipo real e outra tem tipo inteiro, o retorno será
                // de tipo inteiro
                if ((retorno == AlgumaGrammar.REAL && aux == AlgumaGrammar.INTEIRO)
                        || (retorno == AlgumaGrammar.INTEIRO && aux == AlgumaGrammar.REAL))
                    retorno = AlgumaGrammar.REAL;
                // Caso contrário, os tipos são incompatíveis
                else {
                    adicionarErroSemantico(ctx.start, "Termo " + ctx.getText() + " contem tipos incompativeis");
                    retorno = AlgumaGrammar.INVALIDO;
                }

            }
        }
        return retorno;
    }

    // Verifica os tipos dos Fatores Aritméticos
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.FatorContext ctx) {
        AlgumaGrammar retorno = null;

        for (var parcela : ctx.parcela()) {
            retorno = verificarTipo(tabela, parcela);
            // Caso algum termo da parcela seja inválido, interrompe a verificação
        }
        return retorno;
    }

    // Vreificação de tipo de Parcela
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.ParcelaContext ctx) {
        // Identifica se é uma parcela unária ou não unária
        if (ctx.parcela_unario() != null)
            return verificarTipo(tabela, ctx.parcela_unario());
        else
            return verificarTipo(tabela, ctx.parcela_nao_unario());
    }

    // Verificação do tipo de Parcela Unária
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Parcela_unarioContext ctx) {
        AlgumaGrammar retorno;
        String nome;

        // Verificação de variáveis (IDENT - identificador)
        if (ctx.identificador() != null) {
            nome = ctx.identificador().getText();

            // Se o identificador (variável) já tiver sido declarado, apenas retorna seu
            // tipo
            if (tabela.existe(nome))
                retorno = tabela.verificar(nome);
            /*
             * Percorre os escopos aninhados em busca da variável e retorna o tipo. Caso não
             * exista em nenhum deles, retorna o tipo inválido
             */
            else {
                TabelaDeSimbolos tabelaAux = AlgumaSemantico.escoposAninhados.percorrerEscoposAninhados()
                        .get(AlgumaSemantico.escoposAninhados.percorrerEscoposAninhados().size() - 1);
                if (!tabelaAux.existe(nome)) {
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

    // Verifica o tipo de Parcelas Não Unárias
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela,
            AlgumaGrammarParser.Parcela_nao_unarioContext ctx) {
        AlgumaGrammar retorno;
        String nome;

        if (ctx.identificador() != null) {
            nome = ctx.identificador().getText();
            // Verifica se o identificador existe na tabela de símbolos e retorna o tipo
            if (!tabela.existe(nome)) {
                retorno = AlgumaGrammar.INVALIDO;
            } else
                retorno = tabela.verificar(ctx.identificador().getText());
        } else
            retorno = AlgumaGrammar.LITERAL;

        return retorno;
    }

    // Verifica o tipo de Expressões
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.ExpressaoContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.termo_logico(0));

        // Para expressões lógicas, a ideia resume-se apenas em verificar se os tipos
        // analisados são diferentes
        for (AlgumaGrammarParser.Termo_logicoContext termoLogico : ctx.termo_logico()) {
            AlgumaGrammar tipoAtual = verificarTipo(tabela, termoLogico);
            if (retorno != tipoAtual && tipoAtual != AlgumaGrammar.INVALIDO)
                retorno = AlgumaGrammar.INVALIDO;
        }

        return retorno;
    }

    // Verifica o tipo de Termo lógico
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Termo_logicoContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.fator_logico(0));

        for (AlgumaGrammarParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
            AlgumaGrammar tipoAtual = verificarTipo(tabela, fatorLogico);
            if (retorno != tipoAtual && tipoAtual != AlgumaGrammar.INVALIDO)
                retorno = AlgumaGrammar.INVALIDO;
        }
        return retorno;
    }

    // Verifica o tipo de Fator lógico
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Fator_logicoContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.parcela_logica());
        return retorno;

    }

    // Verifica o tipo de Parcela lógica
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Parcela_logicaContext ctx) {
        AlgumaGrammar retorno;

        if (ctx.exp_relacional() != null)
            retorno = verificarTipo(tabela, ctx.exp_relacional());
        else
            retorno = AlgumaGrammar.LOGICO;

        return retorno;

    }

    // Verifica o tipo de Expressão Relacional
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, AlgumaGrammarParser.Exp_relacionalContext ctx) {
        AlgumaGrammar retorno = verificarTipo(tabela, ctx.exp_aritmetica().get(0));

        if (ctx.exp_aritmetica().size() > 1) {

            // Verifica o tipo da expressão aritmética
            AlgumaGrammar tipoAtual = verificarTipo(tabela, ctx.exp_aritmetica().get(1));

            // Semelhante ao que foi feito com as expressões aritméticas, ocorre uma
            // verificação
            // para saber se a expressão atual pode ser tratada como uma operação lógica.
            if (retorno == tipoAtual || verificaCompatibilidade(retorno, tipoAtual))
                retorno = AlgumaGrammar.LOGICO;
            else
                retorno = AlgumaGrammar.INVALIDO;
        }

        return retorno;

    }

    // Verifica o tipo da variável na tabela de símbolos
    public static AlgumaGrammar verificarTipo(TabelaDeSimbolos tabela, String nomeVar) {
        return tabela.verificar(nomeVar);
    }
}
