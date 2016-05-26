#!/bin/bash

# set path to tesseract bin directory
TESSERACT=/usr/local/Cellar/tesseract/3.04.01_1/bin

programname=$0

function usage {
    echo "usage: $programname [-num num_images] [-l lang] [-f font]"
    echo "	-num,--num_images	number of images"
    echo "	-l,--lang			language"
    echo "	-f,--font 			font"
    exit 1
}

usage

# parse arguments
while [[ $# > 1 ]]
do
key="$1"

case $key in
    -num|--num_imgs)
    NUM_IMGS="$2"
    shift # past argument
    ;;
    -l|--lang)
    LANG="$2"
    shift # past argument
    ;;
    -f|--font)
    FONT="$2"
    shift # past argument
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done

echo NUMBER OF IMAGES = "${NUM_IMGS}"
echo LANGUAGE = "${LANG}"
echo FONT = "${FONT}"

# create box files
for i in $(seq 1 $NUM_IMGS);
	do
		$TESSERACT/tesseract $LANG.$FONT.exp$i.tiff $LANG.$FONT.exp$i batch.nochop makebox
done

#correcting the box files

# feed box files to tesseract
for i in $(seq 1 $NUM_IMGS);
	do
		$TESSERACT/tesseract $LANG.$FONT.exp$i.tiff $LANG.$FONT.exp$i.box nobatch box.train.stderr
done

# extract unicharset
$TESSERACT/unicharset_extractor *.box

# create shapetable
$TESSERACT/shapeclustering -F font_properties -U unicharset *.tr

# create training data
$TESSERACT/mftraining -F font_properties -U unicharset -O $LANG.unicharset *.tr
$TESSERACT/cntraining *.tr

# generate dawg files from corresponding word lists
$TESSERACT/wordlist2dawg words_list $LANG.word-dawg $LANG.unicharset
$TESSERACT/wordlist2dawg frequent_words_list $LANG.freq-dawg $LANG.unicharset

# rename files before combining them
mv normproto $LANG.normproto
mv inttemp $LANG.inttemp
mv pffmtable $LANG.pffmtable
mv shapetable $LANG.shapetable
mv unicharset $LANG.unicharset
mv word-dawg $LANG.word-dawg
mv freq-dawg $LANG.freq-dawg

# combine all files to generate traineddata
$TESSERACT/combine_tessdata $LANG.

# move traineddata to tessdata directory
sudo mv $LANG.traineddata /usr/local/share/tessdata