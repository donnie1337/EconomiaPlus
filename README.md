# EconomiaPlus

Plugin de economia para **Paper 26.3**, com sistema próprio de **Coins**, carteira por jogador, banco, ranking e uma loja configurável pelo servidor.

O projeto é independente e utiliza uma API própria de economia.

## Requisitos

| Item | Versão |
|---|---|
| Minecraft | **26.3** |
| Servidor | **Paper 26.3** |
| Java | **25 ou superior** |
| Build | Maven |

## Como compilar

```bash
mvn clean package
```

O arquivo final será gerado em:

```text
target/EconomiaPlus-1.0.0.jar
```

## Comandos

| Comando | Descrição | Permissão | Padrão |
|---|---|---|---|
| `/coins` | Mostra seu saldo de Coins | `coinseconomy.coins` | todos |
| `/coins <jogador>` | Mostra o saldo de outro jogador | `coinseconomy.coins` | todos |
| `/coins top` | Atalho para o ranking de Coins | `coinseconomy.top` | todos |
| `/coins give <jogador> <quantidade>` | Adiciona Coins a um jogador | `coinseconomy.admin` | op |
| `/coins set <jogador> <quantidade>` | Define o saldo de um jogador | `coinseconomy.admin` | op |
| `/pagar <jogador> <quantidade>` | Transfere Coins entre jogadores | `coinseconomy.pagar` | todos |
| `/cobrar <jogador> <quantidade>` | Remove Coins de um jogador | `coinseconomy.cobrar` | op |
| `/topcoins` | Abre o ranking dos jogadores com mais Coins | `coinseconomy.top` | todos |
| `/banco` | Abre o Banco de Coins | `coinseconomy.banco` | todos |
| `/loja` ou `/shop` | Abre a Loja de Coins | `coinseconomy.loja` | todos |

## Carteira e transações

Cada jogador possui sua própria carteira de Coins.

As operações importantes são registradas no histórico de transações, incluindo transferências e operações realizadas pela Loja de Coins.

O sistema usa UUID para identificar os jogadores, evitando perda de saldo em trocas de nome.

## Banco de Coins

O comando `/banco` abre a interface bancária do EconomiaPlus.

O banco possui interface própria e exibe informações financeiras do jogador, operações disponíveis e histórico relacionado à economia.

## Ranking

O `/topcoins` exibe os jogadores com maior quantidade de Coins.

Os valores utilizam formatação compacta, por exemplo:

- `100K Coins`
- `10M Coins`
- `1B Coins`

## Loja de Coins

O comando `/loja` abre uma GUI com categorias de itens.

A loja possui preços separados para **compra** e **venda**. Quando uma operação não está disponível para determinado item, ela simplesmente não aparece na descrição do item.

### Categorias

A loja atualmente possui categorias como:

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

### Regras de venda

Para controlar a geração de Coins e manter a economia mais difícil, o jogador só pode vender itens das seguintes categorias:

- **Plantações**
- **Drops de Mobs**
- **Minérios**

As outras categorias são destinadas apenas à compra.

Os preços de venda dessas três categorias são propositalmente baixos para dificultar a geração rápida de dinheiro e reduzir inflação.

### Minérios

A categoria de Minérios funciona como **somente venda**.

O jogador não pode comprar minérios na loja, mas pode vender minérios, barras, recursos e blocos de armazenamento, incluindo exemplos como:

- Diamante e Bloco de Diamante
- Ouro e Bloco de Ouro
- Ferro e Bloco de Ferro
- Esmeralda e Bloco de Esmeralda
- Cobre
- Carvão
- Lápis-Lazúli
- Redstone
- Netherita
- Recursos brutos

### Blocos Coloridos

A categoria de Blocos Coloridos mantém os itens agrupados por família, facilitando a navegação:

- Concretos
- Pós de concreto
- Lãs
- Terracotas
- Terracotas esmaltadas
- Vidros
- Painéis de vidro
- Carpetes
- Corantes
- Velas
- Camas
- Estandartes
- Caixas de Shulker

As **16 Caixas de Shulker coloridas** custam **250.000 Coins** cada e não podem ser vendidas para a loja.

## Configuração da loja

Os itens e preços ficam em:

```text
src/main/resources/shop.yml
```

Valores de compra ou venda menores ou iguais a zero desativam aquela operação.

Isso permite alterar preços e disponibilidade sem modificar a lógica principal da GUI.

## Armazenamento

Os dados da economia são armazenados pelo plugin e associados ao UUID de cada jogador.

O sistema foi desenvolvido para manter as operações de economia centralizadas no EconomiaPlus.

## Segurança econômica

A loja foi configurada para funcionar principalmente como um **Coin sink**:

- preços de compra são significativamente maiores que os preços de venda;
- somente Plantações, Drops de Mobs e Minérios geram Coins através de venda;
- itens de construção, decoração, utilitários e itens coloridos não podem ser vendidos de volta;
- operações inválidas ou indisponíveis são bloqueadas;
- as transações da loja são registradas no histórico.

Essas regras ajudam a evitar geração excessiva de Coins e tornam a progressão econômica mais lenta.

## Planejado: comércio entre jogadores

Está planejado um sistema de comércio através de `/pw` ou `/comercio`.

A ideia é permitir que jogadores criem lojas físicas utilizando **baús e placas**, porém somente dentro do próprio terreno.

Exemplo visual de uma placa:

```text
Barra de Ferro
x16
C $100K | V $10K
xCebola
```

Onde:

- `C` representa o preço para comprar da loja;
- `V` representa o preço para vender para a loja;
- o baú controla o estoque;
- Coins são transferidos diretamente entre os jogadores;
- o sistema deverá validar estoque, espaço no inventário e saldo;
- a loja só poderá ser criada dentro de um terreno pertencente ao dono;
- compras e vendas poderão gerar mensagens de confirmação e histórico de transações.

Exemplo de mensagem:

```text
SUCESSO • Você vendeu x1 Chave Básica por $70K para xCebola!
```

## Permissões

`coinseconomy.*` concede todas as permissões administrativas e de uso do plugin.

Permissões disponíveis:

- `coinseconomy.coins`
- `coinseconomy.pagar`
- `coinseconomy.cobrar`
- `coinseconomy.top`
- `coinseconomy.banco`
- `coinseconomy.loja`
- `coinseconomy.admin`
