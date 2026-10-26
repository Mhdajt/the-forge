"""Turn any video into the_forge video.fgv  (needs ffmpeg).  Usage: python make_video.py input.mp4 [fps] [quality 2-10]"""
import glob, os, struct, subprocess, sys, tempfile
src = sys.argv[1]; fps = int(sys.argv[2]) if len(sys.argv) > 2 else 30; q = sys.argv[3] if len(sys.argv) > 3 else "5"
tmp = tempfile.mkdtemp()
subprocess.check_call(["ffmpeg", "-y", "-i", src, "-vf", f"fps={fps},scale=1920:1080", "-q:v", q, os.path.join(tmp, "f_%05d.jpg")])
files = sorted(glob.glob(os.path.join(tmp, "f_*.jpg")))
with open("video.fgv", "wb") as out:
    out.write(b"FGV1"); out.write(struct.pack(">ii", fps, len(files)))
    for f in files:
        d = open(f, "rb").read(); out.write(struct.pack(">i", len(d))); out.write(d)
print("wrote video.fgv with", len(files), "frames")
