# CampusFace API - ZL devs

## 👥 Integrantes do Grupo (ZL devs)
* **Gabriel Meira**
* **Gabriel Barbosa**
* **Gabriela Fiori**
* **Heitor Morais**
* **Henrique Cesar**

---

## 📝 Descrição do Projeto
O **CampusFace** é uma solução completa desenvolvida para orquestrar o controle de acesso em instituições (Hubs/Campus), composta por três camadas principais:

* **Backend (Kotlin/Spring Boot):** Orquestra o controle de acesso, gerenciando identidades, permissões e validações.
* **Edge Service (Python):** Totens de reconhecimento facial que processam localmente os embeddings faciais.
* **Frontend (Kotlin Multiplatform):** Interface multiplataforma para gerenciamento de perfis, hubs, aprovações e geração de QR Codes.

O sistema utiliza uma arquitetura híbrida com **Google Firestore** (NoSQL) para flexibilidade e velocidade, **Cloudinary** para armazenamento de imagens faciais, e **ChromaDB** no edge para gerenciamento de embeddings.

---

## 🛠️ Tecnologias Utilizadas

### Backend
* **Linguagem:** Kotlin (JVM 21)
* **Framework:** Spring Boot 3.5.4
* **Banco de Dados:** Google Firebase Firestore (NoSQL)
* **Armazenamento de Imagens:** Cloudinary
* **Documentação:** SpringDoc OpenAPI (Swagger)
* **Segurança:** Spring Security + JWT (HMAC256)

### Frontend
* **Linguagem:** Kotlin (Versão mais recente)
* **Framework:** Compose Multiplatform
* **Plataformas:** Android, iOS, Desktop (JVM), Web
* **Arquitetura:** MVVM com ViewModel
* **Build Tool:** Gradle (Versão mais recente)

### Edge Service
* **Linguagem:** Python 3.9.6
* **Banco de Embeddings:** ChromaDB

---

## 🛠️ Edge Service - Server de reconhecimento local
> **Importante:** Conforme alinhado e autorizado pelo professor, neste projeto **utilizamos um sistema feito em Python como Edge Computing, utilizando o ChromaDB para fazer o gerenciamento dos embeddings**.<br />
> Dessa forma optamos por fazer um sistema interno de gerenciamento das imagens dos rostos, com 5 nomenclaturas importantes<br />
> `Change Request`: Uma solicitação de mudança de imagem, que, quando aceita, é propagada nos servidores.<br />
> `Entry Request`: Uma solicitação de entrada, que, quando aceita, é propagada nos servidores.<br />
> `CheckIn`: Uma operação executada a cada X tempo, para sincronizar os eventos acima.<br />
> `Upsert`: Uma operação enviada ao servidor de reconhecimento, que, insere um elemento se não existir, ou altera caso exista.<br />
> `Delete`: Uma operação enviada ao servidor de reconhecimento, que, deleta um elemento.<br />
### Diagrama de sequência
> Diagrama que exemplifica o fluxo de Change Request e Entry Request, dentro do ecossistema.<br />
> A operação de *CheckIn* é executada diariamente, e somente fornece os dados do servidor de reconhecimento ao backend.<br />

![Diagrama](docs/diagrama.png)

---

## 🚀 Instruções de Instalação e Execução (Backend)

### Pré-requisitos
* Java JDK 21 instalado.
* Arquivo `firebase.json` (Credenciais de serviço do Google) na pasta `src/main/resources/`.
* Variáveis de ambiente configuradas (ou hardcoded no `application.properties` para dev).

### Passos para Executar

1.  **Clonar o repositório:**
    ```bash
    git clone https://github.com/riqalves/campus-face-springboot-kotlin.git
    cd campus-face-springboot-kotlin
    ```

2.  **Configurar Variáveis:**
    Certifique-se de que o `application.properties` ou variáveis de sistema contenham:
    * `CLOUDINARY_URL`
    * `JWT_SECRET`

3.  **Compilar e Rodar:**
    Via terminal (Linux/Mac):
    ```bash
    ./gradlew bootRun
    ```
    Via terminal (Windows):
    ```bash
    .\gradlew.bat bootRun
    ```

4.  **Acessar a Documentação (Swagger):**
    Após iniciar, acesse: `http://localhost:8080/swagger-ui.html`

---

## 🚀 Instruções de Instalação e Execução (Edge Service)

### Pré-requisitos
* Python 3.9.6 instalado
* CMake instalado
* Ngrok instalado e configurado na máquina (https://ngrok.com)

### Passos para Executar
1.  **Clonar o repositório:**
    ```bash
    git clone https://github.com/riqalves/campus-face-springboot-kotlin.git
    cd campus-face-springboot-kotlin/edge
    ```

2.  **Configurar e rodar:**

    Selecione o comando apropriado para o seu sistema operacional:

    <details open>
    <summary><strong>Linux / macOS</strong></summary>

    ```bash
    python3 -m venv venv        # Criação do ambiente
    source venv/bin/activate    # Ativação
    pip install -r requirements.txt # Instala dependências (se houver)
    python main.py
    ```
    </details>

    <details>
    <summary><strong>Windows (PowerShell/CMD)</strong></summary>

    ```powershell
    python -m venv venv         # Criação do ambiente
    .\venv\Scripts\activate     # Ativação
    pip install -r requirements.txt # Instala dependências (se houver)
    python main.py
    ```
    </details>

    ### Argumentos
    
    Você deve personalizar a execução utilizando as seguintes flags:
    
    | Flag        | Descrição                                  | 
    | :---        | :---                                       | 
    | `--backend` | URL base do servidor backend               | 
    | `--email`   | Email do validador para login              | 
    | `--password`| Senha do validador                         | 
    | `--hub`     | Código do Hub para envio do check-in       | 
    | `--port`    | Porta local da aplicação (Flask)           |
    
    **Exemplo de uso customizado:**
    
    ```bash
    python main.py --port 8080 --hub "laboratorio-A" --backend "http://localhost:3000"
---

## 🚀 Instruções de Instalação e Execução (Frontend - Kotlin Multiplatform)

### Pré-requisitos
* Java JDK 21 instalado
* Gradle (versão mais recente)
* Kotlin (versão mais recente)
* **Para iOS:** Xcode instalado (somente macOS)
* **Para Android:** Android SDK configurado
* **Para Desktop/Web:** JVM 21

### Estrutura do Projeto
O frontend utiliza a estrutura padrão do Kotlin Multiplatform:
```
frontend/
├── composeApp/
│   ├── commonMain/      # Código compartilhado entre todas as plataformas
│   ├── androidMain/     # Código específico Android
│   ├── iosMain/         # Código específico iOS
│   ├── desktopMain/     # Código específico Desktop
│   └── wasmJsMain/      # Código específico Web
└── ...
```

### Funcionalidades Principais
* **Gerenciamento de Perfil:** Visualização e edição de dados pessoais
* **Criação de Hubs:** Criação e gerenciamento de organizações/campus
* **Aprovação de Solicitações:** Aprovação de entry requests e change requests
* **QR Code Dinâmico:** Geração e leitura de QR Codes com tempo de expiração para validação de acesso
* **Troca de Foto de Perfil:** Upload e atualização de imagem facial
* **Exibição de Dados:** Visualização de membros, permissões e histórico
* **Funcionalidades Administrativas:** CRUDs das entidades do sistema

### Passos para Executar

1.  **Navegar até o diretório do frontend:**
    ```bash
    cd frontend
    ```

2.  **Configurar Endpoint do Backend:**
    Edite o arquivo `Constants.kt` localizado em `commonMain` e atualize o endpoint do servidor:
    ```kotlin
    object Constants {
        const val BASE_URL = "http://seu-servidor:8080/api" // Atualize aqui
    }
    ```

3.  **Executar por Plataforma:**

    <details open>
    <summary><strong>Desktop (JVM)</strong></summary>

    ```bash
    ./gradlew :composeApp:run
    ```
    </details>

    <details>
    <summary><strong>Android</strong></summary>

    Opção 1 - Via Android Studio:
    * Abra o projeto na pasta `frontend`
    * Selecione a configuração `androidApp`
    * Clique em Run

    Opção 2 - Via linha de comando:
    ```bash
    ./gradlew :composeApp:installDebug
    ```
    </details>

    <details>
    <summary><strong>iOS (macOS apenas)</strong></summary>

    Opção 1 - Via Xcode:
    * Abra o projeto iOS gerado em `iosApp/iosApp.xcodeproj`
    * Selecione o simulador ou dispositivo
    * Clique em Run

    Opção 2 - Via linha de comando:
    ```bash
    ./gradlew :composeApp:iosSimulatorArm64Run
    ```
    </details>

    <details>
    <summary><strong>Web (WASM)</strong></summary>

    ```bash
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
    Acesse: `http://localhost:8080`
    </details>

4.  **Build para Produção:**

    Para gerar builds de produção:
    ```bash
    # Android APK
    ./gradlew :composeApp:assembleRelease
    
    # Desktop (executável)
    ./gradlew :composeApp:packageDistributionForCurrentOS
    
    # iOS (via Xcode)
    # Abra o Xcode e faça Archive
    ```

---

## 🎥 Demonstração do Projeto

Assista ao vídeo de demonstração completo do **CampusFace** em funcionamento:

**🔗 [Link do vídeo](https://youtu.be/FthAte2-Or8)**

> O vídeo apresenta todas as funcionalidades do sistema integrado: Backend (API REST), Edge Service (Reconhecimento Facial) e Frontend Multiplataforma (Android, iOS, Desktop e Web).

---

## ⚠️ Nota sobre Persistência de Dados
> **Importante:** Conforme alinhado e autorizado pelo professor, este projeto **não utiliza JPA/Hibernate com banco relacional**.
>
> Em substituição, utilizamos o **Google Firestore**, um banco de dados NoSQL orientado a documentos. Portanto, as anotações `@Entity`, `@Table` e interfaces `JpaRepository` foram substituídas pelas implementações do SDK do Firebase Admin e anotações de serialização nativas.

---

## ✅ Atendimento aos Requisitos do Projeto

Abaixo detalhamos como cada item da entrega foi implementado no código:

### 1) Projeto Base Spring Boot com Kotlin
O projeto foi criado utilizando Gradle com Kotlin DSL, contendo dependências `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-security` e `springdoc-openapi`.

#### a) Implementação de Entidades
**Como alinhado e autorizado previamente** foram criadas 7 entidades, todas com ID e variados tipos de dados (String, Instant, Enum, Boolean, List):

1.  **User** :
    * Campos: `id` (String), `fullName` (String), `email` (String), `createdAt` (Instant).
2.  **Organization**:
    * Campos: `id` (String), `adminIds` (List), `createdAt` (Instant).
3.  **EntryRequest**:
    * Campos: `status` (Enum RequestStatus), `requestedAt` (Instant).
4.  **AuthCode**:
    * Campos: `valid` (**Boolean**), `expirationTime` (Instant), `code` (String).
5.  **OrganizationMember**:
    * Campos: `role` (Enum Role), `status` (Enum MemberStatus).
6.  **ChangeRequest**:
    * Campos: `newFaceImageId` (String), `status` (Enum RequestStatus), `requestedAt` (Instant).
7.  **RegisteredClient**:
    * Campos: `ipAddress` (String), `lastCheckin` (Instant), `status` (Enum ClientStatus).
#### b) Repositórios (Adaptação Firestore)
Como utilizamos Firestore, os repositórios são classes anotadas com `@Repository` que manipulam as `Collections`.
* Exemplo: `UserRepository`  define métodos como `save`, `findById`, `findByEmail`.
* Exemplo: `OrganizationRepository` manipula a coleção `"organizations"`.

#### c) Classes de Serviços (CRUD)
Lógica de negócios implementada nas classes `@Service`:
* **Criar:** `OrganizationService.createOrganization`, `UserService.createUser`.
* **Ler:** `OrganizationMemberService.getAllMembers`, `UserService.getUserById`.
* **Atualizar:** `OrganizationService.updateOrganization`, `OrganizationMemberService.updateMemberRole`.
* **Remover:** `OrganizationController.delete`, `OrganizationMemberService.removeMember`.

#### d) Controladores REST
Controladores expõem rotas completas. O destaque é o **`OrganizationController`**):
* `POST /organizations`: Criação.
* `GET /organizations`: **Listar todos**.
* `GET /organizations/{id}`: **Pegar por ID**.
* `PUT /organizations/{id}`: Atualização.
* `DELETE /organizations/{id}`: Remoção.

#### e) Validações com Bean Validation
Utilizamos anotações do `jakarta.validation` nos DTOs e `@Valid` nos Controllers.


#### f) Documentação API (Swagger/OpenAPI)
A documentação é gerada automaticamente pelo `springdoc-openapi`.
* Configuração: `/configuration/OpenApiConfig.kt`.
* Acesso: `/swagger-ui.html`

* As capturas de tela dos endpoints funcionando (requests e responses) devem ser anexadas separadamente na entrega conforme solicitado.






"# campus-face-PI-FATEC" 
