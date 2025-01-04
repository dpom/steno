import argparse
import cv2
import numpy as np
import matplotlib.pyplot as plt

# Create an image with text on it
# img = np.zeros((100,400),dtype='uint8')
# font = cv2.FONT_HERSHEY_SIMPLEX
# cv2.putText(img,'TheAILearner',(5,70), font, 2,(255),5,cv2.LINE_AA)
# img1 = img.copy()

ap = argparse.ArgumentParser()
ap.add_argument("-i", "--image", required=True, help="path to input image")
args = vars(ap.parse_args())

img = cv2.imread(args["image"], 0)
invert = cv2.bitwise_not(img)
img1 = invert.copy()
 
# Structuring Element
kernel = cv2.getStructuringElement(cv2.MORPH_CROSS,(3,3))
# Create an empty output image to hold values
thin = np.zeros(img.shape,dtype='uint8')

# Loop until erosion leads to an empty set
while (cv2.countNonZero(img1)!=0):
    # Erosion
    erode = cv2.erode(img1,kernel)
    # Opening on eroded image
    opening = cv2.morphologyEx(erode,cv2.MORPH_OPEN,kernel)
    # Subtract these two
    subset = erode - opening
    # Union of all previous sets
    thin = cv2.bitwise_or(subset,thin)
    # Set the eroded image for next iteration
    img1 = erode.copy()
    
# cv2.imshow('original',img)
# cv2.imshow('thinned',thin)
# cv2.waitKey(0)
plt.imshow(thin, cmap="gray")
plt.show()
