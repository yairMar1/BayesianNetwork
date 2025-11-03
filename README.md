# Bayesian Network Inference Engine

A Java based program designed to perform probabilistic inference on Bayesian Networks. This project parses network structures from standard XML files and answers complex probabilistic queries using several distinct inference algorithms, including the well known **Variable Elimination** algorithm.

## Example Usage

The program runs on an input file containing a series of queries. The first line specifies the network file to load, and subsequent lines contain the queries to be answered. The number at the end of a conditional query (e.g., `,1`, `,2`, `,3`) specifies which inference algorithm to use.

**Sample `input.txt`:**

alarm_net.xml
P(B=F,E=T,A=T,M=T,J=F)
P(B=T|J=T,M=T),1
P(B=T|J=T,M=T),2
P(B=T|J=T,M=T),3
P(J=T|B=T),1
P(J=T|B=T),2
P(J=T|B=T),3

**Expected Console Output:**

0.00004,0,4
0.28417,7,32
0.28417,7,16
0.28417,7,16
0.84902,15,64
0.84902,7,12
0.84902,5,8

> **Explanation of the output:** The two numbers following the probability (e.g., `7,32`) represent the exact count of **additions and multiplications** performed. This was a key project requirement designed to benchmark algorithm performance in a way that is **independent of the underlying hardware**, providing a fair and consistent comparison.

## Key Features

- **Standard Network Parsing:** Loads Bayesian Networks from XML files.
- **Complex Query Processing:** Parses and answers two primary types of probabilistic queries:
    1.  **Joint Probability:** e.g., `P(B=F,E=T,A=T,M=T,J=F)`
    2.  **Conditional Probability:** e.g., `P(B=T|J=T,M=T)`
- **Multiple Inference Algorithms:** Implements several different algorithms to solve the same query, allowing for a direct comparison of their computational cost.

## Technical Highlights & Capabilities Demonstrated

This project showcases a deep understanding of the algorithms that power probabilistic graphical models.

### 1. Robust Network and Query Parser
The system includes a robust parser that can:
- **Read and build a graph data structure** from a structured XML file, correctly creating nodes and storing their Conditional Probability Tables (CPTs).
- **Parse complex query strings** to correctly identify the query variables, evidence variables, and their specified states. This requires careful string manipulation and logical processing.

### 2. Implementation and Benchmarking of Inference Algorithms
The core of this project is the from scratch implementation of algorithms to answer probabilistic queries, with a strong focus on performance analysis.
- **Variable Elimination:** A sophisticated algorithm that dramatically reduces the number of calculations required by intelligently eliminating variables one by one.
- **Algorithmic Benchmarking:** A key feature is the precise counting of arithmetic operations. This approach **normalizes performance measurement**, allowing for a fair comparison of algorithmic efficiency that is not affected by CPU speed or other hardware variations.
- **Why it matters:** This demonstrates a mature engineering mindset. It's not just about making the code work, but about **quantitatively analyzing its efficiency**. This skill is critical for writing high-performance code and making informed decisions about which algorithm is best suited for a given problem.

### 3. Data Structures for Probabilistic Models
The project required designing and implementing custom data structures in Java to represent the core components of a Bayesian Network:
- **`Variable`**: Represents a node in the graph.
- **`Factor`**: A flexible data structure used to represent the Conditional Probability Tables (CPTs) and the intermediate results during the Variable Elimination process.
- **`BayesianNetwork`**: The main graph structure that holds all the variables and their relationships.