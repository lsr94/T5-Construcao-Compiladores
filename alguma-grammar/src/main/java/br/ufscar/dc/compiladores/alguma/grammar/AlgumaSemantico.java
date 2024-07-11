package br.ufscar.dc.compiladores.alguma.grammar;


import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.DeclaracoesContext;
import main.java.br.ufscar.dc.compiladores.alguma.grammar.Escopos;
import main.java.br.ufscar.dc.compiladores.alguma.grammar.AlgumaSemanticoUtils;
import main.java.br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos;
import main.java.br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.TipoAlguma;

public class AlgumaSemantico extends AlgumaGrammarBaseVisitor<Double> {
    
    TabelaDeSimbolos tabela;
    
    static Escopos escoposAninhados = new Escopos();

    TabelaDeSimbolos tabelaEscopos;

    @Override
    public Void visitPrograma(AlgumaGrammarParser.ProgramaContext ctx) {
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }

    //
    @Override
    public Void visitVariavel(AlgumaParser.VariavelContext ctx) {
        String nomeVar = ctx.VARIAVEL().getText();
        String strTipoVar = ctx.TIPO_VAR().getText();
        TipoAlguma tipoVar = TipoAlguma.INVALIDO;
        switch (strTipoVar) {
            case "INTEIRO":
                tipoVar = TipoAlguma.INTEIRO;
                break;
            case "REAL":
                tipoVar = TipoAlguma.REAL;
                break;
            default:
                // Nunca irá acontecer, pois o analisador sintático
                // não permite
                break;
        }
    
    // mudamos aqui ja!
    public Void visitDeclaracao(DeclaracoesContext ctx){

        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.Decl_local_globalContext declaracao: ctx.decl_local_global())
            visitDecl_local_global(declaracao);

        return super.visitDeclaracoes(ctx);

        

    //     String nomeVar = ctx.VARIAVEL().getText();
    //     String strTipoVar = ctx.TIPO_VAR().getText();

    //     TipoAlguma tipoVar = TipoAlguma.INVALIDO;

        
    //     switch(strTipoVar){
    //         case "INTEIRO":
    //             tipovar = TipoAlguma.INTEIRO;
    //         case "REAL":
    //             tipovar = TipoAlguma.REAL;
    //         case "LITERAL":
    //             tipovar = TipoAlguma.LITERAL;
    //         case "BOOL":
    //             tipovar = TipoAlguma.BOOL;
    //     }

    //     // Verificar se a variável já foi declarada
    //     if (tabela.existe(nomeVar)) {
    //         AlgumaSemanticoUtils.adicionarErroSemantico(ctx.VARIAVEL().getSymbol(), "Variável " + nomeVar + " já existe");
    //     } else {
    //         tabela.adicionar(nomeVar, tipoVar);
    //     }

    //     return super.visitDeclaracoes(ctx);
    }

    @Override
    public Void visitComandoAtribuicao(AlgumaGrammarParser.CmdAtribuicaoContext ctx) {
        
        
        
        TipoAlguma tipoExpressao = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.Exp_aritmetica());
        if (tipoExpressao != TipoAlguma.INVALIDO) {
            String nomeVar = ctx.IDENT().getText();
            if (!tabela.existe(nomeVar)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
            } else {
                TipoAlguma tipoVariavel = AlgumaSemanticoUtils.verificarTipo(tabela, nomeVar);
                if (tipoVariavel != tipoExpressao) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "Tipo da variável " + nomeVar + " não é compatível com o tipo da expressão");
                }
            }
        }
        return super.visitCmdAtribuicao(ctx);
    }

    @Override
    public Void visitComandoChamada(AlgumaGrammarParser.CmdChamadaContext ctx) {
        TipoAlguma tipoChamada = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.Exp_aritmetica());
        if (tipoExpressao != TipoAlguma.INVALIDO) {
            String nomeVar = ctx.IDENT().getText();
            if (!tabela.existe(nomeVar)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
            } else {
                TipoAlguma tipoVariavel = AlgumaSemanticoUtils.verificarTipo(tabela, nomeVar);
                if (tipoVariavel != tipoExpressao) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "Tipo da variável " + nomeVar + " não é compatível com o tipo da expressão");
                }
            }
        }
        return super.visitCmdChamada(ctx);
    }

    @Override
    public Void visitComandoEntrada(AlgumaGrammarParser.CmdLeiaContext ctx) {
        String nomeVar = ctx.IDENT().getText();
        if (!tabela.existe(nomeVar)) {
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
        }
        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitExpressaoAritmetica(AlgumaGrammarParser.Exp_aritmeticaContext ctx) {
        AlgumaSemanticoUtils.verificarTipo(tabela, ctx);
        return super.visitExp_aritmetica(ctx);
    }

}