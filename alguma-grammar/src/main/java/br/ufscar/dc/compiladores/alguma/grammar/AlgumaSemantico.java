package br.ufscar.dc.compiladores.alguma.grammar;

import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.IdentificadorContext;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.ParcelaContext;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.Parcela_nao_unarioContext;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.Parcela_unarioContext;
import br.ufscar.dc.compiladores.alguma.grammar.Escopos;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaSemanticoUtils;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.AlgumaGrammar;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.TipoEntrada;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.Token;

public class AlgumaSemantico extends AlgumaGrammarBaseVisitor<Void> {
    TabelaDeSimbolos tabela;
    TabelaDeSimbolos tabelaEscopos;

    static Escopos escoposAninhados = new Escopos();

    // Criação de uma tabela que armazenará as variáveis referentes a um registro
    static HashMap<String, ArrayList<String>> tabelaRegistro = new HashMap<>();

    
    // Criação de uma tabela que armazenará os nomes e parâmetros das funções e procedimentos
    static HashMap<String, ArrayList<AlgumaGrammar>> tabelaFuncaoProcedimento = new HashMap<>();

    @Override
    public Void visitPrograma(AlgumaGrammarParser.ProgramaContext ctx) {
        // Inicialização do programa.
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }

    // Adiciona variável e seu tipo na tabela de símbolos
    public void adicionaVariavelTabela(String nomeVariavel, String tipoVariavel, Token nomeToken, Token TipoToken, TipoEntrada tipoEnt) {
        tabelaEscopos = escoposAninhados.obterEscopoAtual();

        AlgumaGrammar tipoItem;
        boolean flagPonteiro = false;

        // Checa se o tipo da variável inicia com ^, identificando um ponteiro
        if (tipoVariavel.startsWith("^")) {
            // Caso sim, define a flag de ponteiro como verdadeira e remove o caractere ^ do tipo
            flagPonteiro = true;
            tipoVariavel = tipoVariavel.substring(1); 
        }

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
            case "logico":
                tipoItem = AlgumaGrammar.LOGICO;
                break;
            case "registro":
               tipoItem = AlgumaGrammar.REGISTRO;
                break;
            default:
                tipoItem = AlgumaGrammar.INVALIDO;
                break;
        }

        // Verificando se o tipo da variável é 'INVÁLIDO' para retornar mensagem de erro
        if (tipoItem == AlgumaGrammar.INVALIDO) {
            tabelaEscopos.adicionar(nomeVariavel, tipoItem, tipoEnt, flagPonteiro);
            AlgumaSemanticoUtils.adicionarErroSemantico(TipoToken, "tipo " + tipoVariavel + " nao declarado");
        }

        /*
         * Se o tipo da variável não for 'INVÁLIDO', verifica se ela já existe
         * Caso não exista: adiciona
         * Caso exista: retorna erro semântico, pois já foi declarada
         */
        else if (!tabelaEscopos.existe(nomeVariavel))
            tabelaEscopos.adicionar(nomeVariavel, tipoItem, tipoEnt, flagPonteiro);
        else
            AlgumaSemanticoUtils.adicionarErroSemantico(nomeToken, "identificador " + nomeVariavel + " ja declarado anteriormente");

    }

    // Visita as declarações
    @Override
    public Void visitDeclaracoes(AlgumaGrammarParser.DeclaracoesContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.Decl_local_globalContext declaracao : ctx.decl_local_global())
            visitDecl_local_global(declaracao);

        return super.visitDeclaracoes(ctx);
    }

    // Visita as declarações locais
    @Override
    public Void visitDeclaracao_local(AlgumaGrammarParser.Declaracao_localContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        String tipoVariavel;
        String nomeVariavel;
        // Caso 1: Declaração de variável
        if (ctx.getText().startsWith("declare")) {
            
            //System.out.println(ctx.variavel().identificador());

            // Caso 1.1: A declaração possui definição de registro
            // Etapa: Para cada atributo do registro, adiciona 'variavel.atributo' com tipo do atributo
            // na tabela de símbolo
            if (ctx.variavel().tipo().registro() != null) {
                System.out.println("Entrou no Caso 1.1!");
                for (var variavel : ctx.variavel().identificador()){
                    //System.out.println(variavel.getText());
                    for(var variavel_registro: ctx.variavel().tipo().registro().variavel()){
                        for (var ident: variavel_registro.identificador()){
                            //System.out.println("Ident variavel registro:"+ident.getText());
                            //System.out.println("Tipo variavel registro:"+variavel_registro.tipo().getText());
                            adicionaVariavelTabela(variavel.getText() + "." + ident.getText(), variavel_registro.tipo().getText(), ident.getStart(), variavel_registro.tipo().getStart(), TipoEntrada.VARIAVEL);
                        }
                    }      
                }
            // Caso 1.2: A declaração não possui definição de novo registro        
            } else { 
                System.out.println("Entrou no caso 1.2!");
                tipoVariavel = ctx.variavel().tipo().getText();
                //System.out.println(tipoVariavel);
                // Caso 1.2.1: verifica se o tipo é um registro já declarado
                if (tabelaRegistro.containsKey(tipoVariavel)) {
                    System.out.println("Entrou no caso 1.2.1!");
                    ArrayList<String> variavelRegistro = tabelaRegistro.get(tipoVariavel);

                    for (AlgumaGrammarParser.IdentificadorContext ic: ctx.variavel().identificador()) {
                        nomeVariavel = ic.IDENT().get(0).getText();

                        if (tabela.existe(nomeVariavel) || tabelaRegistro.containsKey(nomeVariavel)) 
                            AlgumaSemanticoUtils.adicionarErroSemantico(ic.getStart(), "identificador " + nomeVariavel + " ja declarado anteriormente");
                        else {
                            adicionaVariavelTabela(nomeVariavel, "registro", ic.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);

                            for (int i = 0; i < variavelRegistro.size(); i = i + 2){
                                adicionaVariavelTabela(nomeVariavel + "." + variavelRegistro.get(i), variavelRegistro.get(i+1), ic.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);
                                System.out.println(nomeVariavel +"." + variavelRegistro.get(i)+ " TIPO: "+ variavelRegistro.get(i+1));
                            }
                        }
                    }
                } else { // Caso 1.2.2: verifica se é um tipo básico já declarado
                    System.out.println("Entrou no caso 1.2.2!");
                    for (AlgumaGrammarParser.IdentificadorContext identc : ctx.variavel().identificador()) {
                        nomeVariavel = identc.getText();

                        // Verifica se pode ser uma função ou procedimento
                        if (tabelaFuncaoProcedimento.containsKey(nomeVariavel))
                            AlgumaSemanticoUtils.adicionarErroSemantico(identc.getStart(), "identificador " + nomeVariavel + " ja declarado anteriormente");
                        else
                            adicionaVariavelTabela(nomeVariavel, tipoVariavel, identc.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL); 
                    }
                }
            }
        // Caso 2: Declaração de tipo
        } else if (ctx.getText().contains("tipo")) { // Caso 3: verifica se é um novo tipo
            if (ctx.tipo().registro() != null) {
                ArrayList<String> variaveisRegistro = new ArrayList<String>();
                // Itera sobre os atributos do tipo
                for (AlgumaGrammarParser.VariavelContext vc : ctx.tipo().registro().variavel()) {
                    tipoVariavel = vc.tipo().getText();
                    
                    for (AlgumaGrammarParser.IdentificadorContext ic : vc.identificador()) {
                        System.out.println("Ident variavel registro:"+ic.getText());
                        System.out.println("Tipo variavel registro:"+tipoVariavel);
                        // Adiciona o IDENT e o tipo da variável
                        variaveisRegistro.add(ic.getText());
                        variaveisRegistro.add(tipoVariavel);
                    }
                }
                //System.out.println(variaveisRegistro);
                // Adiciona o tipo aos hashmap de registros
                tabelaRegistro.put(ctx.IDENT().getText(), variaveisRegistro);
            }
        // Caso 3: Constante    
        } else if (ctx.getText().contains("constante")){
            adicionaVariavelTabela(ctx.IDENT().getText(), ctx.tipo_basico().getText(), ctx.IDENT().getSymbol(), ctx.IDENT().getSymbol(), TipoEntrada.VARIAVEL);
        // Caso 4: a declaração é uma variável   
        
        }
        return super.visitDeclaracao_local(ctx);
    }


   // Visita registro e cada uma das variáveis
    @Override 
    public Void visitRegistro(AlgumaGrammarParser.RegistroContext ctx) {
        
        return visitChildren(ctx); 
    
    }


    @Override
    public Void visitDecl_local_global(AlgumaGrammarParser.Decl_local_globalContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        // Identifica se é uma declaração local ou global
        if (ctx.declaracao_local() != null) {
            visitDeclaracao_local(ctx.declaracao_local());
        } else if (ctx.declaracao_global() != null)
            visitDeclaracao_global(ctx.declaracao_global());

        return super.visitDecl_local_global(ctx);
    }

    // Visita atribuições
    @Override
    public Void visitCmdAtribuicao(AlgumaGrammarParser.CmdAtribuicaoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        String nomeVariavel = ctx.identificador().getText();
        //System.out.println("Visitando atribuição! Variável+" + ctx.identificador());
        boolean deferenciacao = ctx.start.getText().equals("^");
        boolean ref_memoria = ctx.expressao().getText().startsWith("&");
        // System.out.println("Inicio da expressao: "+ctx.start.getText()+".// ^?"+isDereferenced);
        AlgumaGrammar tipoExpressao = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());

        if (tipoExpressao != AlgumaGrammar.INVALIDO) {
            // Caso a variável não tenha sido declarada, informa o erro
            if (!tabela.existe(nomeVariavel)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
            } else {
                // Caso tenha sido declarada, verifica os demais casos
                AlgumaGrammar tipoVar = tabela.verificar(nomeVariavel);
                boolean isPointerVar = tabela.verificar_ponteiro(nomeVariavel);
                //System.out.println("Nome variavel: " + nomeVariavel + ". Tipo variavel:" + tipoVar + ". Pointer?" + isPointerVar);
                //System.out.println("Expressao: " + ctx.expressao() + " . Texto expressao: " + ctx.expressao().getText() + ". Tipo Expressao:" + tipoExpressao);

                // Se a variável é um ponteiro há 2 casos:
                if (isPointerVar){
                    /* Caso 1: Há deferenciação. Ex: distancia é ponteiro de int. ^distancia <- 400
                        * Testar se o tipo da variável é o mesmo da expressão.
                        * Testar se a variável é ponteiro.
                        * Erro? incompatível ^ */
                    if (deferenciacao && !ref_memoria) {
                        if (tipoVar != tipoExpressao)
                            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.start.getText() + nomeVariavel);
                    }    
                    /* Caso 2: Atribuir o endereço da expressão à variável.
                    * Verificar se há o símbolo &
                    * Verificar se a expressão possui o mesmo tipo da variável
                    * Verificaar se a variável é ponteiro */
                    else if (ref_memoria && !deferenciacao){
                        String identificadorExpressao = extractIndentificadorString(ctx.expressao().getText());
                        // Caso a variável seja um ponteiro e o identificador da expressão não seja null
                        if (identificadorExpressao != null) {
                            // Verifica se o identificador da expressão existe na tabela de símbolos e são
                            // de mesmo tipo
                            AlgumaGrammar tipoId = tabela.verificar(identificadorExpressao);
                            if (tipoId != tipoVar) {
                                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + nomeVariavel);
                            }
                        }
                    }
                    // Exceções: Erro.
                    else{
                        AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + nomeVariavel);
                    }
                }
                // Caso a variável não seja ponteiro, verifica apenas o tipo
                // Atribuição entre expressão de tipo inteiro para variável real é compatível
                else if (!(tipoExpressao.name() == "INTEIRO" && tipoVar.name() == "REAL" ) && tipoExpressao != tipoVar){
                    System.out.println("INCOMPATIVEL. Expressao de tipo: "+tipoExpressao+". Tipo var:"+tipoVar);
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "tipo de atribuicao incompativel para " + nomeVariavel);
                }
            }
        }
        return super.visitCmdAtribuicao(ctx);
    }

    private String extractIndentificadorString(String expression) {
        // Extrai o identificador após o símbolo '&''
        if (!expression.startsWith("&"))
            return null;

        return expression.substring(1).trim().split("\\s+")[0];
    }

    // Visita comandos de leitura
    @Override
    public Void visitCmdLeia(AlgumaGrammarParser.CmdLeiaContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.IdentificadorContext id : ctx.identificador())
            if (!tabela.existe(id.getText()))
                AlgumaSemanticoUtils.adicionarErroSemantico(id.getStart(), "identificador " + id.getText() + " nao declarado");

        return super.visitCmdLeia(ctx);
    }

    // Visita parcelas
    @Override
    public Void visitParcela(ParcelaContext ctx) {
       return super.visitParcela(ctx);
    }


    // Visita parcelas não unárias
    @Override
    public Void visitParcela_nao_unario(Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            IdentificadorContext idc = ctx.identificador();
            for (var id : idc.IDENT()) {
                String nomeVar = id.getText();
                //System.out.println("Parcela Unaria. Nome var: " + nomeVar);
                boolean declarado = false;
                for (var tabela : escoposAninhados.percorrerEscoposAninhados()) {
                    if (tabela.existe(nomeVar)) {
                        declarado = true;
                    }
                }
                if (!declarado) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(id.getSymbol(), "identificador " + nomeVar + " nao declarado");
                }
            }
        }
        // Caso o if acima não seja satisfeito, a parcela não unária é uma cadeira
        return super.visitParcela_nao_unario(ctx);
    }
    // Visita parcelas unárias
    @Override
    public Void visitParcela_unario(Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            IdentificadorContext idc = ctx.identificador();
            for (var id : idc.IDENT()) {
                String nomeVar = id.getText();
                //System.out.println("Parcela Unaria. Nome var: " + nomeVar);
                boolean declarado = false;
                for (var tabela : escoposAninhados.percorrerEscoposAninhados()) {
                    System.out.println("Nome var da parcela:"+nomeVar);
                    if (tabela.existe(nomeVar)) {
                        declarado = true;
                    }
                }
                if (!declarado) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(id.getSymbol(), "identificador " + nomeVar + " nao declarado");
                }
            }
        }

        return super.visitParcela_unario(ctx);
    }

    @Override
    public Void visitFator_logico(AlgumaGrammarParser.Fator_logicoContext ctx) {
        return super.visitFator_logico(ctx);
    }

    @Override
    public Void visitTermo_logico(AlgumaGrammarParser.Termo_logicoContext ctx) {
        return super.visitTermo_logico(ctx);
    }

    @Override
    public Void visitTermo(AlgumaGrammarParser.TermoContext ctx) {
        return super.visitTermo(ctx);
    }

    @Override
    public Void visitFator(AlgumaGrammarParser.FatorContext ctx) {
        return super.visitFator(ctx);
    }


    @Override
    public Void visitCmdSe(AlgumaGrammarParser.CmdSeContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        var conditions = ctx.expressao().termo_logico().get(0);
        //var expressions = ctx.expressao().termo_logico().get(1);


        // Leitura dos termos

        // Caso não haja o operador "ou" (op_logico_1) 
        // => há apenas termos lógicos (fatores lógicos e op_logico_2 )
        if (ctx.expressao().op_logico_1().isEmpty()){
            System.out.println(conditions.fator_logico().size());
            
            // Lê cada condição do comando 'Se'
            for (var fator_logico: conditions.fator_logico()){
                System.out.println("\t\tImprimindo 'fator'"+fator_logico.getText());
                
                // Varre cada uma das expressões
                for (var exps: fator_logico.parcela_logica().exp_relacional().exp_aritmetica()){
                    for (var termo: exps.termo()){
                        System.out.println("\t\t\t\tImprimindo 'termo'"+termo.getText());
                        // Testar se é CADEIA 
                        
                        
                        // Testar outros tipos: ...
                    }
                    //.fator().parcela().parcela_unario().IDENT.getText());
                    
                }
            }

        }
        // Há operador 'ou':
        else{
            System.out.println("To-do");
        }
        // for (var fator: conditions.fator_logico()){

        //     System.out.println(fator.getText());
        // }
        // for (var termos: ctx.expressao().termo_logico()){
            
        //     System.out.println(termos.getText());
        // }
        


        AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());

        return super.visitCmdSe(ctx);
    }

    @Override
    public Void visitCmdEnquanto(AlgumaGrammarParser.CmdEnquantoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());

        return super.visitCmdEnquanto(ctx);
    }
}