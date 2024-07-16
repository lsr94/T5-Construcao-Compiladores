package br.ufscar.dc.compiladores.alguma.grammar;

import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.alguma.grammar.Escopos;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaSemanticoUtils;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.AlgumaGrammar;
import org.antlr.v4.runtime.Token;

public class AlgumaSemantico extends AlgumaGrammarBaseVisitor<Void> {
    TabelaDeSimbolos tabela;
    
    static Escopos escoposAninhados = new Escopos();

    TabelaDeSimbolos tabelaEscopos;

    @Override
    public Void visitPrograma(AlgumaGrammarParser.ProgramaContext ctx) {
        // Inicialização do programa.
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }

    public void adicionaVariavelTabela(String nomeVariavel, String tipoVariavel, Token nomeToken, Token TipoToken) {
        tabelaEscopos = escoposAninhados.obterEscopoAtual();

        AlgumaGrammar tipoItem;

        switch (tipoVariavel) {
            case "literal":
                tipoItem = AlgumaGrammar.LITERAL;
                break;
            case "inteiro":
                tipoItem = AlgumaGrammar.INTEIRO;
                break;
            case "real":
                tipoItem = AlgumaGrammar.REAL;
                break;
            case "booleano":
                tipoItem = AlgumaGrammar.BOOL;
            default:
                tipoItem = AlgumaGrammar.INVALIDO;
                break;
        }

        // Verificando se o tipo da variável é 'INVÁLIDO' para retornar mensagem de erro
        if (tipoItem == AlgumaGrammar.INVALIDO)
            AlgumaSemanticoUtils.adicionarErroSemantico(TipoToken, " tipo " + tipoVariavel + " nao declarado");
        /* Se o tipo da variável não for 'INVÁLIDO', verifica se ela já existe
        Caso não exista: adiciona
        Caso exista: retorna erro semântico, pois já foi declarada */
        else if (!tabelaEscopos.existe(nomeVariavel))
            tabelaEscopos.adicionar(nomeVariavel, tipoItem);
        else
            AlgumaSemanticoUtils.adicionarErroSemantico(nomeToken, " identificador " + nomeVariavel + " ja declarado");

    }

    @Override
    public Void visitDeclaracoes(AlgumaGrammarParser.DeclaracoesContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.Decl_local_globalContext declaracao: ctx.decl_local_global())
            visitDecl_local_global(declaracao);

        return super.visitDeclaracoes(ctx);
    }

    @Override
    public Void visitDeclaracao_local(AlgumaGrammarParser.Declaracao_localContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        String tipoVariavel;
        String nomeVariavel;

        if (ctx.getText().contains("declare")) {
            tipoVariavel = ctx.variavel().tipo().getText();

            // Adiciona a variável atual na tabela (a verificação de variável repetida ocorre no método adicionaVariavelTabela)
            for (AlgumaGrammarParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                nomeVariavel = ident.getText();
                adicionaVariavelTabela(nomeVariavel, tipoVariavel, ident.getStart(), ctx.variavel().tipo().getStart());
            }
        }

        return super.visitDeclaracao_local(ctx);
    }


    @Override
    public Void visitDecl_local_global(AlgumaGrammarParser.Decl_local_globalContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        
        // Identifica se é uma declaração local ou global
        if (ctx.declaracao_local() != null){
            System.out.print("Entrou na local!");    
            visitDeclaracao_local(ctx.declaracao_local());
        }
        else if (ctx.declaracao_global() != null)
            System.out.print("Entrou na global!");    
        //visitDeclaracao_global(ctx.declaracao_global());

        return super.visitDecl_local_global(ctx);
    }

    @Override
    public Void visitCmdAtribuicao(AlgumaGrammarParser.CmdAtribuicaoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        
        AlgumaGrammar tipoExpressao = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());
        
        String nomeVariavel = ctx.identificador().getText();
        
        if (tipoExpressao != AlgumaGrammar.INVALIDO) {
            // Caso a variável não tenha sido declarada, informa o erro
            if (!tabela.existe(nomeVariavel)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
            } else {
                // Caso tenha sido declarada, verifica os demais casos
                AlgumaGrammar varTipo = AlgumaSemanticoUtils.verificarTipo(tabela, nomeVariavel);
                
                // Caso o tipo seja inteiro ou real, é utilizada a função verificaCompatibilidade para verificar
                // se o valor a ser trabalhado é real ou não (mais informações sobre a função podem ser encontradas
                // no arquivo T3SemanticoUtils.java.
                if (varTipo == AlgumaGrammar.INTEIRO || varTipo == AlgumaGrammar.REAL) {
                    if (!AlgumaSemanticoUtils.verificaCompatibilidade(varTipo, tipoExpressao)) {
                        // Caso o tipo da expressão (restante da parcela sendo analisada) seja diferente de inteiro,
                        // não é possível tratar o valor como um número real, logo, os tipos são incompatíveis, pois
                        // seria a situação de estar comparando um número com um literal, por exemplo.
                        if (tipoExpressao != AlgumaGrammar.INTEIRO) {
                            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), " atribuicao nao compativel para " + ctx.identificador().getText());
                        }
                    }
                // Caso a expressão analisada não tenha números que precisem ser tratados de maneira especial,
                // apenas verifica se os tipos são diferentes.
                } else if (varTipo != tipoExpressao)
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), " atribuicao nao compativel para " + ctx.identificador().getText());
            }
        }
        
        return super.visitCmdAtribuicao(ctx);
    }

    @Override
    public Void visitCmdLeia(AlgumaGrammarParser.CmdLeiaContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.IdentificadorContext id : ctx.identificador())
            if (!tabela.existe(id.getText()))
                AlgumaSemanticoUtils.adicionarErroSemantico(id.getStart(), "identificador " + id.getText() + " nao declarado");

        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitCmdEscreva(AlgumaGrammarParser.CmdEscrevaContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        
        for (AlgumaGrammarParser.ExpressaoContext expressao : ctx.expressao())
            AlgumaSemanticoUtils.verificarTipo(tabela, expressao);

        return super.visitCmdEscreva(ctx);
    }

    @Override
    public Void visitCmdEnquanto(AlgumaGrammarParser.CmdEnquantoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        
        AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());
        
        return super.visitCmdEnquanto(ctx);
    }
}