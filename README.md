# 365FinalProj
R u a youtuber? This project classifies the types of transitions you may have used

## Steps:

### create sti images (video frame data across axis versus time)
1. copy middle column of each frame into image
2. copy middle row of each frame into image (turned sideways like a column)

ex: 120x160 video with 100 frames
center column sti: 120 x 100
center row sti: 160 x 100 (rows turned sideways)

### finding the cut
1. take sti image and create a chromaticity histogram {r,g} = {R,G}/(R+G+B). (watch out for black!)
2. use r and g as axis for 2d histogram NXNxvalue size
3. #bins N=floor(1+log2n)     n=rows
4. histogram each column in the grame
5. compare histogram at time t with t-1
6. create a threshold

edit test