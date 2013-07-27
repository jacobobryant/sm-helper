#!/usr/bin/python
# This is a script that I used to create the raw text files included in my
# SM Helper android app.

import urllib.request
from progressbar import ProgressBar
from lxml import html, etree
from tempfile import TemporaryFile
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter
from sys import stdout, stderr

INDEX_URL = "http://www.lds.org/manual/seminary/student-resource?lang=eng"

def url_to_doc(url):
    content = urllib.request.urlopen(url).read()
    tmp = TemporaryFile()
    tmp.write(content)
    tmp.seek(0)
    tree = html.parse(tmp)
    tmp.close()
    return tree

def get_verses(link):
    cmd = 'descendant::text()[not(ancestor::sup)]'
    verses = url_to_doc(link).xpath('//p[@class="highlight"]')
    return [''.join(verse.xpath(cmd)) for verse in verses]

def build_scripture(link):
    content = url_to_doc(link.get('href')).xpath(
            '//div[@id="primary"]/div[@class="topic"]')[0]
    verseLink = content.xpath('h2/a/attribute::href')[0]
    verses = get_verses(verseLink)
    reference = link.text
    keywords = ''.join(content.xpath('p//child::text()'))
    context = ''.join(content.xpath('div[1]/p//child::text()'))
    doctrine = ''.join(content.xpath('div[2]/p//child::text()'))
    application = ''.join(content.xpath('div[3]/p//child::text()'))
    return "\t".join([reference, keywords, context, doctrine,
        application] + verses)

if __name__ == "__main__":
    parser = ArgumentParser(description="Downloads the scripture " +
            "mastery scriptures from lds.org and saves them in a text " +
            "file.", formatter_class=ArgumentDefaultsHelpFormatter)
    parser.add_argument('outfile', nargs='?',
            help="The output file (default: stdout)")
    parser.add_argument('--progress', '-p', action="store_true",
            default=False, help="Display progress.")
    parser.add_argument('--testing', '-t', action="store_true",
            default=False, help="Test run.")
    args = parser.parse_args()
    outfile = open(args.outfile, "w") if args.outfile != None else stdout

    if args.progress:
        i = 1
        pbar = ProgressBar().start()
    output = ""
    doc = url_to_doc(INDEX_URL)
    bookLinks = doc.xpath(
            '//div[@id="primary"]/ul/li[2]/p/a')
    for link in bookLinks:
        if output != "":
            output += "\n"
        output += link.text + "\n"
        doc = url_to_doc(link.get('href'))
        referenceLinks = doc.xpath(
                '//div[@id="primary"]/table/tr[position()>2]/td[2]/a')
        for refLink in referenceLinks:
            output += build_scripture(refLink) + "\n"
            if args.progress:
                pbar.update(i)
                i+=1
            if args.testing:
                break
        if args.testing:
            break
    if args.progress:
        pbar.finish()
    print(output, file=outfile)
    if outfile is not stdout:
        outfile.close()
