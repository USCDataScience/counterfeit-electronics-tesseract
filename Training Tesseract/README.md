# Training Tesseract

You can use the training script or follow the steps below in order to train Tesseract locally.

## Running the training script:
You can use [train.sh](https://github.com/USCDataScience/counterfeit-electronics-tesseract/blob/master/Training%20Tesseract/train.sh) to train Tesseract with any language and different types of font styles as well as font sizes. 

Usage:
```
./train.sh [-l lang] [-tesspath tesseract_path] [-datapath trainingdata_path]
    -l,--lang			    language
    -tesspath               path to tesseract bin directory
    -datapath               path to training data directory
```

The training process does not require correction of box files fed to Tesseract but for improving the accuracy of the system, it is advisable to correct the box files generated during the training process before feeding it back.

## Steps to train Tesseract:

### Create Training document(s):
Create a file with text in the language, font style and size as required for training. It should preferably have a line spacing of 1.5 and 1pt spacing between the characters. Save this as a pdf file with the name as [lang].font-name.exp0.pdf, with lang being an ISO-639 three letter abbreviation for your language and then convert it to a 300dpi tiff file. (you can use imagemagick for this).

The following is the command to convert the file using imagemagick:
```
convert -density 300 -depth 4 lang.font-name.exp0.pdf lang.font-name.exp0.tif
```
If you’re adding multiple fonts, or bold, italic or underline, repeat this process multiple times, creating one doc → pdf → tiff per font variation.

### Train tesseract using the generated documents:

* Create box files using the above-generated tiff files through the following command:
```
tesseract lang.font-name.exp0.tiff lang.font-name.exp0 batch.nochop makebox
```

This command generates a box file for the input tiff file. The box file is a UTF-8 encoded file containing the x,y coordinates of the boxes containing each letter. Each letter is placed on a separate line. Here, Tesseract would’ve recognised each character in the tiff file, some of them may be correct while the others may be wrong. Also, it maybe possible that some letters have been merged as one or completely overlooked in the output file. These must be rectified manually in the box file. Go through each letter one by one and identify the ones which have been incorrectly identified, merged or ignored by Tesseract and correct the same. Also modify the x,y coordinates accordingly. Box-file editors may be used to modify them. 

* Feed the box file to Tesseract:
```
tesseract eng.font-name.exp0.tif eng.font-name.box nobatch box.train.stderr
```
* Extract unicharset from the file:
```
unicharset_extractor *.box
```
If the above command gives errors, try running it by specifying the absolute path to the executable. This applies for all the following commands as well.

* Create font_properties file:
It should list every font you’re training, one per line, and identify whether it has the following characteristics: 
```
<fontname> <italic> <bold> <fixed> <serif> <fraktur>
```
For example,
```
eng.arial.box 0 0 0 0 0
```
* Create shapetable:
```
shapeclustering -F font_properties -U unicharset *.tr
```

* Create training data:
```
mftraining -F font_properties -U unicharset -O lang.unicharset *.tr 
cntraining *.tr
```

* Create required DAWG files:
Create a word list and frequent words list for your training language and then run the following commands:
```
wordlist2dawg word_list lang.word-dawg lang.unicharset
wordlist2dawg frequency_list lang.freq-dawg lang.unicharset
```

* Combine the created files:
Rename normproto, Microfeat, inttemp, pffmtable files with language name prefix and run:
```
combine_tessdata lang.
```

* Moving the files to /usr/local/share:
```
sudo mv eng.traineddata /usr/local/share/tessdata/
sudo mv eng.traineddata /usr/local/Cellar/tesseract/{version}/share/tessdata
```

To test Tesseract run the following command with the new language/font as argument:
```
tesseract image.tif output -l lang
```

### Improving Results

Tesseract is trained to detect sentences by default. In order to improve the results with your trained data set (which may be used to detect numeric or alphanumeric texts), the following modifications are possible:

* Supply custom word list to Tesseract instead of standard dictionary it uses.
* Modify tessedit_char_whitelist in tessdata/configs/digits to include only the characters you are interested in extracting.
* Set page segmentation mode in the command line based on where the text is located in the image.
