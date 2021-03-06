% Generated by maven-r-jsr223-plugin: do not edit by hand

\name{CitationGraph}

\alias{CitationGraph}

\title{}

\usage{\preformatted{
library(jlitrev)
J = JavaApi$new();
instance <- J$CitationGraph$new(...);
}}

\description{
	
}

\details{
	
}

\examples{
## -----------------------------------
## Method `CitationGraph$fromPropertyFile`
## -----------------------------------

## -----------------------------------
## Method `CitationGraph$analser`
## -----------------------------------

## -----------------------------------
## Method `CitationGraph$shutdown`
## -----------------------------------

## -----------------------------------
## Method `CitationGraph$buildFromPropertyFile`
## -----------------------------------

## -----------------------------------
## Method `CitationGraph$build`
## -----------------------------------

}

\keyword{java api}

\section{Methods}{
	\subsection{Constructors}{
		\itemize{
			\item \href{#method-new}{\code{J$CitationGraph$new(secrets, working)}}
		}
	}
	\subsection{Static methods}{
		\itemize{
			\item \href{#method-fromPropertyFile}{\code{J$CitationGraph$fromPropertyFile(propFilename)}}
		}
	}
	\subsection{Instance methods}{
		\itemize{
			\item \href{#method-analser}{\code{instance$analser()}}
			\item \href{#method-shutdown}{\code{instance$shutdown()}}
			\item \href{#method-buildFromPropertyFile}{\code{instance$buildFromPropertyFile(propFilename)}}
			\item \href{#method-build}{\code{instance$build(search, broaderSearch, earliest, latest)}}
			\item \code{instance$clone()}
			\item \code{instance$print()}
		}
	}

	\if{html}{\out{<hr>}}
	\if{html}{\out{<a id="method-new"></a>}}
	
	\subsection{Method \code{new()}}{
		Loads a set of configuration parameters from a file and start up a neo4j graph.
	}

	\subsection{Usage}{
		\if{html}{\out{<div class="r">}}
		\preformatted{
  library(jlitrev)
  J <- JavaApi$new()
  J$CitationGraph$new(secrets, working)
		}
		\if{html}{\out{</div>}}
	}
	
	\subsection{Arguments}{
		\if{html}{\out{<div class="arguments">}}
		\describe{
			\itemize{
				\item{secrets}{ - (java expects a java.lang.String)}
				\item{working}{ - (java expects a java.lang.String)}
			}
		}
		\if{html}{\out{</div>}}
	}

	\subsection{Returns}{
		R6 CitationGraph object: 
	}

	\subsection{Examples}{
		\if{html}{\out{<div class="r example copy">}}
		\preformatted{
		}
		\if{html}{\out{</div>}}
	}
	\if{html}{\out{<hr>}}
	\if{html}{\out{<a id="method-fromPropertyFile"></a>}}
	
	\subsection{Method \code{fromPropertyFile()}}{
		
	}

	\subsection{Usage}{
		\if{html}{\out{<div class="r">}}
		\preformatted{
  library(jlitrev)
  J <- JavaApi$new()
  J$CitationGraph$fromPropertyFile(propFilename)
		}
		\if{html}{\out{</div>}}
	}
	
	\subsection{Arguments}{
		\if{html}{\out{<div class="arguments">}}
		\describe{
			\itemize{
				\item{propFilename}{ - (java expects a java.lang.String)}
			}
		}
		\if{html}{\out{</div>}}
	}

	\subsection{Returns}{
		R6 CitationGraph object: 
	}

	\subsection{Examples}{
		\if{html}{\out{<div class="r example copy">}}
		\preformatted{
		}
		\if{html}{\out{</div>}}
	}
	\if{html}{\out{<hr>}}
	\if{html}{\out{<a id="method-analser"></a>}}
	
	\subsection{Method \code{analser()}}{
		
	}

	\subsection{Usage}{
		\if{html}{\out{<div class="r">}}
		\preformatted{
  library(jlitrev)
  J <- JavaApi$new()
  instance <- J$CitationGraph$new(...);
  instance$analser()
		}
		\if{html}{\out{</div>}}
	}
	
	\subsection{Arguments}{
		\if{html}{\out{<div class="arguments">}}
		\describe{
			\itemize{
			}
		}
		\if{html}{\out{</div>}}
	}

	\subsection{Returns}{
		R6 CitationGraphAnalyser object: 
	}

	\subsection{Examples}{
		\if{html}{\out{<div class="r example copy">}}
		\preformatted{
		}
		\if{html}{\out{</div>}}
	}
	\if{html}{\out{<hr>}}
	\if{html}{\out{<a id="method-shutdown"></a>}}
	
	\subsection{Method \code{shutdown()}}{
		
	}

	\subsection{Usage}{
		\if{html}{\out{<div class="r">}}
		\preformatted{
  library(jlitrev)
  J <- JavaApi$new()
  instance <- J$CitationGraph$new(...);
  instance$shutdown()
		}
		\if{html}{\out{</div>}}
	}
	
	\subsection{Arguments}{
		\if{html}{\out{<div class="arguments">}}
		\describe{
			\itemize{
			}
		}
		\if{html}{\out{</div>}}
	}

	\subsection{Returns}{
		void: 
	}

	\subsection{Examples}{
		\if{html}{\out{<div class="r example copy">}}
		\preformatted{
		}
		\if{html}{\out{</div>}}
	}
	\if{html}{\out{<hr>}}
	\if{html}{\out{<a id="method-buildFromPropertyFile"></a>}}
	
	\subsection{Method \code{buildFromPropertyFile()}}{
		
	}

	\subsection{Usage}{
		\if{html}{\out{<div class="r">}}
		\preformatted{
  library(jlitrev)
  J <- JavaApi$new()
  instance <- J$CitationGraph$new(...);
  instance$buildFromPropertyFile(propFilename)
		}
		\if{html}{\out{</div>}}
	}
	
	\subsection{Arguments}{
		\if{html}{\out{<div class="arguments">}}
		\describe{
			\itemize{
				\item{propFilename}{ - (java expects a java.lang.String)}
			}
		}
		\if{html}{\out{</div>}}
	}

	\subsection{Returns}{
		void: 
	}

	\subsection{Examples}{
		\if{html}{\out{<div class="r example copy">}}
		\preformatted{
		}
		\if{html}{\out{</div>}}
	}
	\if{html}{\out{<hr>}}
	\if{html}{\out{<a id="method-build"></a>}}
	
	\subsection{Method \code{build()}}{
		Conducts a build process using the supplied configuration 
Does not check the graph is empty
	}

	\subsection{Usage}{
		\if{html}{\out{<div class="r">}}
		\preformatted{
  library(jlitrev)
  J <- JavaApi$new()
  instance <- J$CitationGraph$new(...);
  instance$build(search, broaderSearch, earliest, latest)
		}
		\if{html}{\out{</div>}}
	}
	
	\subsection{Arguments}{
		\if{html}{\out{<div class="arguments">}}
		\describe{
			\itemize{
				\item{search}{ - (java expects a java.lang.String)}
				\item{broaderSearch}{ - (java expects a java.lang.String)}
				\item{earliest}{ - (java expects a java.lang.String)}
				\item{latest}{ - (java expects a java.lang.String)}
			}
		}
		\if{html}{\out{</div>}}
	}

	\subsection{Returns}{
		void: 
	}

	\subsection{Examples}{
		\if{html}{\out{<div class="r example copy">}}
		\preformatted{
		}
		\if{html}{\out{</div>}}
	}
}