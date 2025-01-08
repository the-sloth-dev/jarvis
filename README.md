
# J.A.R.V.I.S. (Just A Retrieval-Augmented Virtual Intelligent System)

J.A.R.V.I.S. is an intelligent assistant designed to leverage **Retrieval-Augmented Generation (RAG)** for delivering accurate and context-aware responses. 
This project integrates technologies like **Spring AI**, **Llama3**, **Vaadin**, and **PGvector** to build a scalable, efficient, and secure AI-powered assistant.

---

## Features

- **Responsive Chat UI**: Built with **Vaadin** for a modern and user-friendly interface.
- **Retrieval-Augmented Generation (RAG)**: Combines contextual information and memory for precise responses.
- **Private Language Model Hosting**: Powered by **Llama3** hosted via **Ollama**.
- **Vector Search**: Utilizes **PostgreSQL with PGvector** for fast and accurate similarity searches.
- **Structured Ingestion Pipeline**: Processes and stores documents with **Spring AI ETL** tools.
- **Prompt Engineering**: Implements robust system and user prompts for consistent and accurate interactions.

---

## Tech Stack

- **Frontend**: Vaadin
- **Backend**: Java, Spring AI
- **Language Model**: Llama3 (via Ollama)
- **Database**: PostgreSQL with PGvector
- **Containerization**: Docker Compose
- **ETL Tools**: Spring AI components

---

## Getting Started

### Prerequisites

1. **Docker** and **Docker Compose**
2. **Java 23** or later
3. **PostgreSQL** with the **PGvector** extension
4. **Maven** for building the project

---

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/the-sloth-dev/jarvis.git
   cd jarvis
   ```

2. **Start PostgreSQL with PGvector**
   ```bash
   docker-compose up -d pgvector
   ```

3. **Start Ollama with Llama3**
   ```bash
   docker-compose up -d ollama
   ```

   Pull the Llama3 model:
   ```bash
   docker exec -it ollama ollama pull llama3
   ```

4. **Configure the Application**
   Update the `application.yml` file with your environment variables:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5433/postgres
       username: postgres
       password: postgres
     ai:
       ollama:
         base-url: http://localhost:11434
       vectorstore:
         pgvector:
           index-type: HNSW
           distance-type: COSINE_DISTANCE
           dimensions: 768
   ```

5. **Build and Run the Application**
   ```bash
   mvn clean install
   java -jar target/jarvis-0.0.1-SNAPSHOT.jar
   ```

6. **Access the Application**
   Open [http://localhost:8080](http://localhost:8080) in your browser.

---

## Usage

### Trigger Document Ingestion
Place your documents in the directory specified in the configuration (`/tmp/jarvis/documents` by default) and trigger ingestion using the API:
```bash
curl -X POST http://localhost:8080/ingest/run
```

### Chat with the Assistant
Use the chat interface to ask questions based on the ingested documents.

---

## Key Components

### System Prompt
Defines the assistant's behavior and rules for context and memory usage:
```plaintext
You are an intelligent assistant specialized in retrieval-augmented tasks.
Your primary goal is to provide accurate, concise, and relevant answers strictly based on the provided context and memory.
```

### User Prompt
Guides each query to ensure responses adhere to system rules:
```plaintext
<context>
    {question_answer_context}
</context>
User question: {user_question}
```

---

## Contributing

We welcome contributions to improve J.A.R.V.I.S.! Please submit pull requests or report issues via GitHub.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**Author**: [The Sloth Dev](https://github.com/the-sloth-dev)  
*"Building smarter systems, one step at a time!"*
