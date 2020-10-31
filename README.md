# Katholic

> "We may throw the dice—but the Lord determines how they fall." —Proverbs 16:33

Symbolic integration and probabilistic programming in the spirit of [Church](https://web.stanford.edu/~ngoodman/papers/churchUAI08_rev2.pdf) and [Anglican](https://probprog.github.io/anglican/index.html).

# Research Questions

* Is there a probability distribution which can be combined linearly to form an algebra which is closed over integration?
* Is there an efficient symbolic or numerical procedure to run inverse transform sampling on higher dimensional quantiles?
* If not, can we use approximate inference methods like [SVGD](https://www.depthfirstlearning.com/2020/SVGD) or [normalizing flows](https://lilianweng.github.io/lil-log/2018/10/13/flow-based-deep-generative-models.html) to draw samples from probabilistic programs?

# Example

Below, we show the posterior distribution from simple mixture of two Kumaraswamy distributions. The CDF is computed using [Rubi's symbolic integration scheme](https://rulebasedintegration.org/).

![](exact_pdf.svg)

![](exact_cdf.svg)

![](inversion_sampled_pdf.svg)

# References

## Symbolic Methods

* [APPL: A Probability Programming Language](https://www.tandfonline.com/doi/pdf/10.1198/000313001750358509), Glen et al. (2012)
* [Symbolic Statistics with SymPy](http://people.cs.uchicago.edu/~mrocklin/tempspace/sympystats.pdf), Rocklin (2010)
* [PSI: Exact Symbolic Inference for Probabilistic Programs](https://files.sri.inf.ethz.ch/website/papers/psi-solver.pdf), Gehr et al. (2016)
* [λPSI: Exact Inference for Higher-Order Probabilistic Programs](https://files.sri.inf.ethz.ch/website/papers/pldi20-lpsi.pdf), Gehr et al. (2020)
* [Symbolic Maximum Likelihood Estimation with Mathematica](https://rss.onlinelibrary.wiley.com/doi/pdf/10.1111/1467-9884.00233), Rose and Smith (2001)
* [The Harmonic Logarithms and the Binomial Formula](https://core.ac.uk/download/pdf/82415331.pdf), Roman (1993)
* [Closed Form Integration of Artificial Neural Networks](https://escholarship.org/content/qt0wz7n7nm/qt0wz7n7nm.pdf#page=5), Gottschling (1999)
* [A New Distribution on the Simplex with Auto-Encoding Applications](https://papers.nips.cc/paper/9520-a-new-distribution-on-the-simplex-with-auto-encoding-applications.pdf), Stirn et al. (2019)

## Logical Methods

* [A Logical Approach for Factoring Belief Networks](http://reasoning.cs.ucla.edu/fetch.php?id=24&type=pdf), Darwiche (2002)
* [Transforming Probabilistic Programs into Algebraic Circuits for Inference and Learning](https://openreview.net/pdf?id=SygbjU6iBS), Martires et al. (2019)
* [Tractable Operations for Arithmetic Circuits of Probabilistic Models](https://papers.nips.cc/paper/6363-tractable-operations-for-arithmetic-circuits-of-probabilistic-models.pdf), Shen, Choi and Darwiche (2016)
* [Compiling Probabilistic Graphical Models Using Sentential Decision Diagrams](http://reasoning.cs.ucla.edu/fetch.php?id=129&type=pdf), Choi, Kisa and Darwiche (2013)

## Fast Samplers

* [Fast inverse transform sampling in one and two dimensions](https://arxiv.org/pdf/1307.1223.pdf), Olver and Townsend (2013)
* [Fast and accurate parallel quantile computation](https://discovery.ucl.ac.uk/id/eprint/1482128/1/Luu_thesis.pdf#page=12), Luu (2016)
* [Fast Random Integer Generation in an Interval](https://arxiv.org/pdf/1805.10941.pdf), Lemire (2018)

## Normalizing Flows

* [Normalizing Flows for Probabilistic Modeling and Inference](https://arxiv.org/pdf/1912.02762.pdf), Papamakarios et al. (2019)
* [Efficient Inference Amortization in Graphical Models using Structured Continuous Conditional Normalizing Flows](http://proceedings.mlr.press/v108/weilbach20a/weilbach20a.pdf) Weilbach et al. (2020)

## Software

* [JavaBayes](https://github.com/joeschweitzer/javabayes): Bayesian networks in [Java](https://www.cs.cmu.edu/~javabayes/Home/node6.html)
* [Credici](https://github.com/IDSIA/credici): Credal Models Algorithms
* [Crema](https://github.com/IDSIA/crema): Credal Inference for Causal Inference
