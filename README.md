## RUN
Run `ro.racai.robin.dialog.RDManager` giving it a micro-world file, such as the one in `src\main\resources\precis.mw`.

## SSLA dependency
SSLA is a Text-To-Speech library developed by Tiberiu Boroș et al.
Read about it on [arXiv](https://arxiv.org/pdf/1802.05583.pdf). The source code can be found on GitHub at [SSLA](https://github.com/racai-ai/ssla).
MLPLA is the text preprocessing front-end for SSLA.

Install the SSLA TTS library in your local Maven repository by running this command:

`mvn install::install-file -Dfile=speech\ssla\SSLA.jar -DgroupId=ro.racai -DartifactId=ssla -Dversion=1.0.0 -Dpackaging=jar -DgeneratePom=true`

Install the MLPLA text text preprocessing library in your local Maven repository by running this command:

`mvn install::install-file -Dfile=speech\mlpla\MLPLA.jar -DgroupId=ro.racai -DartifactId=mlpla -Dversion=1.0.0 -Dpackaging=jar -DgeneratePom=true`
