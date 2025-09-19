O WePayU é um sistema de informação para administrar a folha de pagamentos de uma empresa. O projeto foi desenvolvido em Java e foca na lógica de negócios, seguindo os requisitos definidos em um conjunto de User Stories e testes de aceitação automatizados.

O sistema gerencia um banco de dados de empregados, cartões de ponto, resultados de vendas, taxas sindicais e processa os pagamentos de acordo com as regras específicas para cada tipo de empregado.

O sistema atualmente suporta as seguintes funcionalidades, baseadas nas User Stories de 1 a 8:

Gestão de Empregados:

Adição de novos empregados (horistas, assalariados e comissionados) com atributos como nome, endereço e salário.

Remoção de empregados do sistema.

Alteração dos detalhes de um empregado, incluindo nome, endereço, tipo, método de pagamento e afiliação sindical.

Lançamentos:

Lançamento de cartões de ponto para empregados horistas.

Lançamento de resultados de vendas para empregados comissionados.

Lançamento de taxas de serviço para membros do sindicato.

Folha de Pagamento:

Cálculo e geração da folha de pagamento para uma data específica, pagando os empregados de acordo com suas agendas e métodos de pagamento.

Funcionalidades Adicionais:

Sistema de Undo/Redo para desfazer e refazer as operações principais (adição, remoção, alteração, etc.).

Persistência de dados em arquivo XML, salvando o estado do sistema ao ser encerrado.

Tecnologias Utilizadas:

Linguagem: Java

Testes: EasyAccept

Pré-requisitos:

Para compilar e executar o projeto, você precisará ter o JDK (Java Development Kit) instalado em sua máquina.

Como Compilar e Executar o Projeto:

1. Compilar o Código
Execute o comando a seguir na raiz do projeto para compilar todos os arquivos fonte. Os arquivos .class serão gerados no diretório out.

PowerShell

#No Windows (usando PowerShell ou CMD)
javac -d out -cp "lib\easyaccept.jar" src\Main.java src\br\ufal\ic\p2\wepayu\*.java src\br\ufal\ic\p2\wepayu\models\*.java src\br\ufal\ic\p2\wepayu\Exception\*.java src\br\ufal\ic\p2\wepayu\Services\*.java

#No Linux ou macOS
javac -d out -cp "lib/easyaccept.jar" src/Main.java src/br/ufal/ic/p2/wepayu/*.java src/br/ufal/ic/p2/wepayu/models/*.java src/br/ufal/ic/p2/wepayu/Exception/*.java src/br/ufal/ic/p2/wepayu/Services/*.java

2. Executar os Testes de Aceitação
Para rodar o sistema e executar os testes de aceitação automatizados (definidos em src/Main.java), utilize o comando abaixo:

PowerShell

#No Windows (usando PowerShell ou CMD)
java -cp "out;lib\easyaccept.jar" Main

#No Linux ou macOS
java -cp "out:lib/easyaccept.jar" Main
O sistema executará os testes contidos nos arquivos us1.txt, us2.txt, etc., que estão na pasta tests.
