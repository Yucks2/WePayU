O WePayU é um sistema de informação para administrar a folha de pagamentos de uma empresa. O projeto foi desenvolvido em Java e foca na lógica de negócios, seguindo os requisitos definidos em um conjunto de User Stories e testes de aceitação automatizados.

O sistema gerencia um banco de dados de empregados, cartões de ponto, resultados de vendas, taxas sindicais e processa os pagamentos de acordo com as regras específicas para cada tipo de empregado.

Tecnologias Utilizadas:

Linguagem: Java

Testes: EasyAccept

Pré-requisitos:

Para compilar e executar o projeto, você precisará ter o JDK (Java Development Kit) instalado em sua máquina.

Como Compilar e Executar o Projeto:

Execute o comando a seguir na raiz do projeto para compilar todos os arquivos fonte. Os arquivos .class serão gerados no diretório out.

PowerShell

#No Windows (usando PowerShell ou CMD)
javac -d out -cp "lib\easyaccept.jar" src\Main.java src\br\ufal\ic\p2\wepayu\*.java src\br\ufal\ic\p2\wepayu\models\*.java src\br\ufal\ic\p2\wepayu\Exception\*.java src\br\ufal\ic\p2\wepayu\Services\*.java

#No Linux ou macOS
javac -d out -cp "lib/easyaccept.jar" src/Main.java src/br/ufal/ic/p2/wepayu/*.java src/br/ufal/ic/p2/wepayu/models/*.java src/br/ufal/ic/p2/wepayu/Exception/*.java src/br/ufal/ic/p2/wepayu/Services/*.java

Para rodar o sistema e executar os testes de aceitação automatizados (definidos em src/Main.java), utilize o comando abaixo:

PowerShell

#No Windows (usando PowerShell ou CMD)
java -cp "out;lib\easyaccept.jar" Main

#No Linux ou macOS
java -cp "out:lib/easyaccept.jar" Main
O sistema executará os testes contidos nos arquivos us1.txt, us2.txt, etc., que estão na pasta tests.
