# EconomiaPlus

EconomiaPlus é um plugin de economia para **Paper 26.3**, desenvolvido para centralizar a moeda **Coins** do servidor em um único sistema.

O plugin possui carteira por jogador, transferências, histórico de movimentações, ranking econômico, título de Magnata, banco com rendimento, loja configurável e uma API própria para integração com outros plugins.

## Requisitos

| Item | Requisito |
|---|---|
| Servidor | Paper 26.3 |
| Java | 25 ou superior |
| Build | Maven |

O plugin não depende de Vault.

## Funcionalidades

### Carteira de Coins

Cada jogador possui uma conta identificada por UUID.

O comando `/coins` abre uma GUI com:

- saldo atual da carteira;
- saldo guardado no banco;
- status de recebimento de pagamentos;
- acesso ao histórico de transações;
- acesso ao ranking;
- informações do Magnata atual.

Ao clicar em **Suas Informações**, o jogador pode ativar ou desativar o recebimento de Coins de outros jogadores.

Quando o recebimento está desativado, comandos como `/pagar` e `/coins pagar` não conseguem enviar Coins para aquele jogador.

### Transferências entre jogadores

Os jogadores podem transferir Coins usando:

```text
/pagar <jogador> <quantidade>
/coins pagar <jogador> <quantidade>
/coins pay <jogador> <quantidade>
```

O sistema:

- impede pagamento para si mesmo;
- valida se o jogador alvo já entrou no servidor;
- valida o valor informado;
- verifica saldo suficiente;
- respeita a preferência de recebimento do destinatário;
- registra a movimentação no histórico de quem enviou e de quem recebeu.

### Histórico de transações

A carteira possui um extrato paginado com até **21 registros por página**.

Atualmente são registrados:

- Coins enviados;
- Coins recebidos;
- depósitos no banco;
- saques do banco;
- adições administrativas;
- remoções administrativas;
- compras na loja;
- vendas para a loja.

O histórico é persistido em disco.

### Ranking de Coins

O comando `/topcoins` abre uma GUI paginada com os jogadores mais ricos do servidor.

Aliases:

```text
/coinstop
/baltop
```

O ranking:

- considera apenas contas com saldo acima de zero;
- ordena do maior para o menor saldo;
- exibe a posição, cabeça do jogador, nome e patrimônio;
- possui paginação;
- também pode ser acessado pela GUI de `/coins`.

Quando executado pelo console, o Top 10 é exibido em texto.

### Magnata

O jogador que ocupa o primeiro lugar da economia é considerado o **Magnata**.

O EconomiaPlus mantém essa informação atualizada sempre que um saldo é alterado.

A API interna do plugin disponibiliza:

- verificação de quem é o Magnata;
- UUID do Magnata atual;
- tag visual `$`;
- tag de chat `[$]`.

A GUI de `/coins` também mostra o jogador que ocupa a posição de Magnata e seu patrimônio.

## Banco

O comando:

```text
/banco
```

abre o sistema bancário do EconomiaPlus.

A reserva bancária é separada do saldo normal da carteira.

### Operações bancárias

O jogador pode:

- depositar Coins da carteira no banco;
- sacar Coins do banco para a carteira;
- consultar o saldo guardado;
- consultar rendimento diário e mensal;
- visualizar o histórico bancário.

Por padrão:

```yaml
banco:
  rendimento-diario-percentual: 0.035
  rendimento-mensal-percentual: 1.05
  rendimento-intervalo-minutos: 60
  operacoes:
    minimo: 1000.0
    limite-saques-diarios: 10
    tempo-limite-segundos: 30
```

O banco possui:

- valor mínimo configurável para operações;
- limite diário configurável de saques;
- contador diário de saques;
- rendimento composto proporcional ao tempo decorrido;
- registro de depósitos, saques e rendimentos;
- histórico bancário paginado.

## Loja de Coins

O comando:

```text
/loja
```

ou:

```text
/shop
```

abre a Loja de Coins.

A loja é totalmente baseada em GUI e possui categorias configuradas em `shop.yml`.

### Categorias atuais

- Plantações
- Utilitários
- Comidas
- Drops de Mobs
- Minérios
- Blocos
- Blocos Coloridos
- Decorações
- Redstone
- Outros

### Compra e venda

Cada item pode possuir um preço de compra e um preço de venda independentes.

Valores menores ou iguais a zero desativam a respectiva operação.

Na interface:

- operações indisponíveis não são exibidas;
- clique esquerdo compra;
- clique direito vende;
- Shift + clique executa operações em quantidade, respeitando saldo, estoque do jogador e espaço no inventário.

Os preços finais de compra são arredondados para Coins inteiros.

### Regras econômicas atuais

Atualmente, somente estas categorias permitem vender itens para o servidor:

- **Plantações**
- **Drops de Mobs**
- **Minérios**

As demais categorias são somente para compra.

A categoria **Minérios** é somente para venda: os jogadores não podem comprar minérios do servidor.

A loja também registra compras e vendas no histórico da carteira.

### Configuração

A loja é configurada em:

```text
plugins/EconomiaPlus/shop.yml
```

Na primeira execução, o arquivo padrão é copiado dos recursos do plugin.

Também existem opções de:

- desconto percentual em compras;
- bônus percentual em vendas.

## Comandos

| Comando | Função |
|---|---|
| `/coins` | Abre a carteira de Coins |
| `/coins <jogador>` | Consulta o saldo de outro jogador |
| `/coins ajuda` | Exibe a ajuda do sistema de Coins |
| `/coins top` | Abre o ranking |
| `/coins pagar <jogador> <quantidade>` | Envia Coins |
| `/coins pay <jogador> <quantidade>` | Alias de pagamento |
| `/coins give <jogador> <quantidade>` | Adiciona Coins administrativamente |
| `/coins add <jogador> <quantidade>` | Alias administrativo de adição |
| `/coins set <jogador> <quantidade>` | Define o saldo de um jogador |
| `/pagar <jogador> <quantidade>` | Transfere Coins |
| `/cobrar <jogador> <quantidade>` | Remove Coins de um jogador |
| `/topcoins` | Abre o ranking econômico |
| `/banco` | Abre o banco |
| `/loja` | Abre a Loja de Coins |
| `/shop` | Alias de `/loja` |

## Administração

### Adicionar Coins

```text
/coins give <jogador> <quantidade>
/coins add <jogador> <quantidade>
```

### Definir saldo

```text
/coins set <jogador> <quantidade>
```

### Cobrar Coins

```text
/cobrar <jogador> <quantidade>
```

O comportamento de cobrança pode ser configurado para permitir ou impedir saldo negativo:

```yaml
cobranca-permite-saldo-negativo: false
```

As alterações administrativas relevantes são registradas no histórico da carteira.

## Permissões

| Permissão | Função | Padrão |
|---|---|---|
| `coinseconomy.coins` | Usar o sistema de Coins | todos |
| `coinseconomy.pagar` | Transferir Coins | todos |
| `coinseconomy.cobrar` | Remover Coins de jogadores | op |
| `coinseconomy.top` | Abrir o ranking | todos |
| `coinseconomy.banco` | Abrir o banco | todos |
| `coinseconomy.loja` | Abrir a loja | todos |
| `coinseconomy.admin` | Usar `/coins give`, `add` e `set` | op |
| `coinseconomy.*` | Todas as permissões | op |

## API própria

O EconomiaPlus registra `EconomyApi` no `ServicesManager` do Bukkit com prioridade alta.

Outros plugins podem obter a API desta forma:

```java
EconomyApi economia = Bukkit.getServicesManager().load(EconomyApi.class);
```

A API expõe operações para:

- verificar existência de conta;
- criar conta;
- consultar saldo;
- verificar saldo suficiente;
- depositar;
- sacar;
- definir saldo;
- consultar nome conhecido;
- obter ranking;
- formatar valores monetários.

## Persistência de dados

O plugin utiliza arquivos YAML na pasta:

```text
plugins/EconomiaPlus/
```

Arquivos principais:

| Arquivo | Conteúdo |
|---|---|
| `data.yml` | Saldos e nomes conhecidos |
| `bank.yml` | Reserva bancária, saques, rendimentos e histórico |
| `wallet-transactions.yml` | Histórico da carteira |
| `wallet-settings.yml` | Preferência de recebimento de pagamentos |
| `shop.yml` | Itens, categorias e preços da loja |
| `config.yml` | Configurações gerais e do banco |

Os dados são salvos automaticamente em intervalo configurável e também durante o desligamento do plugin.

O padrão é:

```yaml
salvamento-automatico-ticks: 6000
```

equivalente a aproximadamente **5 minutos**.

## Migração de versões antigas

Ao iniciar, o EconomiaPlus procura a antiga pasta:

```text
plugins/CoinsEconomy/
```

Caso existam dados antigos e ainda não exista o arquivo equivalente no EconomiaPlus, o plugin migra:

- `config.yml`;
- `data.yml`.

## Configuração padrão

```yaml
saldo-inicial: 0.0

moeda:
  singular: 'coin'
  plural: 'coins'

cobranca-permite-saldo-negativo: false

salvamento-automatico-ticks: 6000

banco:
  rendimento-diario-percentual: 0.035
  rendimento-mensal-percentual: 1.05
  rendimento-intervalo-minutos: 60
  operacoes:
    minimo: 1000.0
    limite-saques-diarios: 10
    tempo-limite-segundos: 30
```

## Build

Clone o projeto e execute:

```bash
mvn clean package
```

O arquivo final será gerado em:

```text
target/EconomiaPlus-1.0.0.jar
```
