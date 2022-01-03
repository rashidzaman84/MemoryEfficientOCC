# Memory Efficient Online Conformance Checking
This prototype implementation is related to the Memory-efficient prefix-alignments based Online Conformance Checking (OCC) approaches of [[1]](#1) and [[2]](#2). The provided prototype implementation
is dependent on the prefix-alignment based Online Conformance package of [[3]](#3) which uses the A<sup>*</sup> algorithm for shortest path search based
prefix-alignment computation. This parent package requires a Petri net process model, its initial marking, and its final marking. Additionally, our approach requires the state
limit as input.

## Installation
 - Download the code to your local machine.
 - Run the "UITopia (MAOCC_States).launch". Let the code to download all the required ProM packages on the first run.
 - Load your event log and the reference process model in the displayed window and run the plugin "01 Compute Prefix Alignments - With Bounded States" with these inputs.
 - The next window requires as input the state limit.
 - The results are displayed on the console.


## References
<a id="1">[1]</a> 
Rashid Zaman, Marwan Hassani, and Boudewijn F. Van Dongen. (2022).
Efficient Memory Utilization in Conformance Checking of Process Event Streams.
In SAC2022. ACM.

<a id="2">[2]</a> 
Rashid Zaman, Marwan Hassani, and Boudewijn F. Van Dongen. (2021).
A Framework for Efficient Memory Utilization in Online Conformance Checking
In arXiv. https://arxiv.org/abs/2112.13640

<a id="3">[3]</a> 
Sebastiaan J van Zelst, Alfredo Bolt, Marwan Hassani, Boudewijn F van Dongen, and Wil MP van der Aalst. (2019).
Online conformance checking: relating event streams to process models using prefix-alignments.
International Journal of Data Science and Analytics 8, 3 (2019), 269–284.
