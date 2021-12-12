import os
import glob
import pandas as pd
import csv
os.chdir("experiments_20200622214935")
extension = 'csv'
all_filenames = [i for i in glob.glob('*.configInfo.{}'.format(extension))]
print(len(all_filenames))
#combine all files in the list
combined_csv = pd.concat([pd.read_csv(f,header=0,error_bad_lines=False,sep=';') for f in all_filenames ])
#export to csv
combined_csv.to_csv( "combined_configinfo_traces.csv")