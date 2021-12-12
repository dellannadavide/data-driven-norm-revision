import os
import glob
import pandas as pd
os.chdir("experiments_20211202133022")
extension = 'csv'
all_filenames = [i for i in glob.glob('*.metrics.{}'.format(extension))]
print(len(all_filenames))
#combine all files in the list
combined_csv = pd.concat([pd.read_csv(f) for f in all_filenames ])
#export to csv
combined_csv.to_csv( "combined.csv", index=False, sep = ",")