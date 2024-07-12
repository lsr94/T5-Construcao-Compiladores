package br.ufscar.dc.compiladores.alguma.grammar;

import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.alguma.grammar.Escopos;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaSemanticoUtils;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.AlgumaGrammar;

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

    // VERIFICAR !!!!!!!
    @Override
    public void adicionaVariavelTabela() {
    
    }
    
    @Override
    public Void visitDeclaracoes(AlgumaGrammarParser.DeclaracoesContext ctx){
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.Decl_local_globalContext declaracao: ctx.decl_local_global())
            visitDecl_local_global(declaracao);

        return super.visitDeclaracoes(ctx);

    @Override
    public Void visitComandoAtribuicao(AlgumaGrammarParser.CmdAtribuicaoContext ctx) {
        AlgumaGrammar tipoExpressao = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.Exp_aritmetica());
        if (tipoExpressao != AlgumaGrammar.INVALIDO) {
            String nomeVar = ctx.identificador().getText();
            if (!tabela.existe(nomeVar)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
            } else {
                AlgumaGrammar tipoVariavel = AlgumaSemanticoUtils.verificarTipo(tabela, nomeVar);
                if (tipoVariavel != tipoExpressao) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getSymbol(), "Tipo da variável " + nomeVar + " não é compatível com o tipo da expressão");
                }
            }
        }
        return super.visitCmdAtribuicao(ctx);
    }

    @Override
    public Void visitComandoChamada(AlgumaGrammarParser.CmdChamadaContext ctx) {
        AlgumaGrammar tipoChamada = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.Exp_aritmetica());
        if (tipoExpressao != AlgumaGrammar.INVALIDO) {
            String nomeVar = ctx.identificador().getText();
            if (!tabela.existe(nomeVar)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
            } else {
                AlgumaGrammar tipoVariavel = AlgumaSemanticoUtils.verificarTipo(tabela, nomeVar);
                if (tipoVariavel != tipoExpressao) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getSymbol(), "Tipo da variável " + nomeVar + " não é compatível com o tipo da expressão");
                }
            }
        }
        return super.visitCmdChamada(ctx);
    }

    @Override
    public Void visitComandoEntrada(AlgumaGrammarParser.CmdLeiaContext ctx) {
        String nomeVar = ctx.identificador().getText();
        if (!tabela.existe(nomeVar)) {
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getSymbol(), "Variável " + nomeVar + " não foi declarada antes do uso");
        }
        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitExpressaoAritmetica(AlgumaGrammarParser.Exp_aritmeticaContext ctx) {
        AlgumaSemanticoUtils.verificarTipo(tabela, ctx);
        return super.visitExp_aritmetica(ctx);
    }
}