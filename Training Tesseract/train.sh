: 'Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
'

#!/bin/bash

function usage {
    echo "usage: $programname [-l lang] [-tesspath tesseract_path] [-datapath trainingdata_path]"
    echo "	-l,--lang			          language"
    echo "  -tesspath               path to tesseract bin directory"
    echo "  -datapath               path to training data directory"
    exit 1
}

# check if input files exist
function check_files_exist {
    while read -r line
    do
        if [[ ! -e $line".tif" ]]; then
            echo "$line.tif does not exist. Exiting now."            
            exit 1
        fi
    done < "$filename"
}

# create box files
function create_box_files {
    while read -r line
    do
        $TESSERACT/tesseract $line.tif $line batch.nochop makebox
        if [ "$?" != 0 ]; then
            echo "Error during creating box files. Exiting now."
            exit 1
        fi
    done < "$filename"    
}

# feed box files to tesseract
function input_box_files {
    while read -r line
    do
        $TESSERACT/tesseract $line.tif $line.box nobatch box.train.stderr
        if [ "$?" != 0 ]; then
            echo "Error during processing box files. Exiting now."
            exit 1
        fi
    done < "$filename"
}

# extract unicharset
function exrtact_unicharset {
    $TESSERACT/unicharset_extractor *.box
    if [ "$?" != 0 ]; then
        echo "Error during extracting unicharset. Exiting now."
        exit 1
    fi
}

# create shapetable
function create_shapetable {
    $TESSERACT/shapeclustering -F font_properties -U unicharset *.tr
    if [ "$?" != 0 ]; then
        echo "Error while creating shapetable. Exiting now."
        exit 1
    fi
}

# create training data
function generate_training_data {
    $TESSERACT/mftraining -F font_properties -U unicharset -O $LANG.unicharset *.tr
    if [ "$?" != 0 ]; then
        echo "Error while creating training data. Exiting now."
        exit 1
    fi
    $TESSERACT/cntraining *.tr
    if [ "$?" != 0 ]; then
        echo "Error while creating training data. Exiting now."
        exit 1
    fi
}

# generate dawg files from corresponding word lists
function generate_dawg_files {
    $TESSERACT/wordlist2dawg words_list $LANG.word-dawg $LANG.unicharset
    if [ "$?" != 0 ]; then
        echo "Error while creating word-dawg data. Exiting now."
        exit 1
    fi

    $TESSERACT/wordlist2dawg frequent_words_list $LANG.freq-dawg $LANG.unicharset
    if [ "$?" != 0 ]; then
        echo "Error while creating freq-dawg data. Exiting now."
        exit 1
    fi    
}

# rename files before combining them
function rename_training_files {
    mv normproto $LANG.normproto
    mv inttemp $LANG.inttemp
    mv pffmtable $LANG.pffmtable
    mv shapetable $LANG.shapetable
    mv unicharset $LANG.unicharset
    mv font_properties $LANG.font_properties
}

# combine all files to generate traineddata
function combine_training_data {
    $TESSERACT/combine_tessdata $LANG.
    if [ "$?" != 0 ]; then
        echo "Error while combining training data. Exiting now."
        exit 1
    fi
}

programname=$0

# parse arguments
if [[ "$#" < 6 ]]; then
    usage
fi

while [[ $# > 1 ]]
do
    key="$1"

    case $key in
        -l|--lang)
        LANG="$2"
        shift # past argument
        ;;
        -tesspath)
        # set path to tesseract bin directory
        TESSERACT="$2"
        shift # past argument
        ;;
        -datapath)
        # set path to training data directory
        DATAPATH="$2"
        shift # past argument
        ;;
        *)
                # unknown option
        ;;
    esac
    shift # past argument or value
done

cd $DATAPATH
ls -a $DATAPATH | grep -i tif | cut -d "." -f 1  > $DATAPATH/files.txt
if [ "$?" != 0 ]; then
    echo "Path entered for data files is incorrect. Exiting now."
    exit 1
fi
filename=$DATAPATH/files.txt

check_files_exist
create_box_files
# correct the box files before feeding back to Tesseract
input_box_files
exrtact_unicharset
create_shapetable
generate_training_data
generate_dawg_files
rename_training_files
combine_training_data

rm $filename

# move traineddata to tessdata directory
sudo mv $LANG.traineddata /usr/local/share/tessdata
