import os
import glob
import pandas as pd
os.chdir("experiments_20200418150701")
extension = 'csv'
all_filenames = [i for i in glob.glob('*.traces.{}'.format(extension))]
print(len(all_filenames))
#combine all files in the list
cols = [9, 10, 11, 12, 25, 26, 27]
combined_csv = pd.concat([pd.read_csv(f,usecols=cols) for f in all_filenames ])
#export to csv
combined_csv.to_csv( "combined_traces.csv")