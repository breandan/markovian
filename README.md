# Katholic

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

* [Symbolic Statistics with SymPy](http://people.cs.uchicago.edu/~mrocklin/tempspace/sympystats.pdf)
* [Fast and accurate parallel quantile computation](https://discovery.ucl.ac.uk/id/eprint/1482128/1/Luu_thesis.pdf#page=12)
* [The Harmonic Logarithms and the Binomial Formula](https://core.ac.uk/download/pdf/82415331.pdf)
* [Closed Form Integration of Artificial Neural Networks](https://escholarship.org/content/qt0wz7n7nm/qt0wz7n7nm.pdf#page=5)
* [Fast inverse transform sampling in one and two dimensions](https://arxiv.org/pdf/1307.1223.pdf)
* [A New Distribution on the Simplex with Auto-Encoding Applications](https://papers.nips.cc/paper/9520-a-new-distribution-on-the-simplex-with-auto-encoding-applications.pdf)

## Normalizing Flows

* [Normalizing Flows for Probabilistic Modeling and Inference](https://arxiv.org/pdf/1912.02762.pdf)
* [Efficient Inference Amortization in Graphical Models using Structured Continuous Conditional Normalizing Flows](http://proceedings.mlr.press/v108/weilbach20a/weilbach20a.pdf)