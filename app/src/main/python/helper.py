from urlextract import URLExtract
from wordcloud import WordCloud
import pandas as pd
from collections import Counter
import emoji
import matplotlib.pyplot as plt
import seaborn as sns
import matplotlib
import io
import base64
import os

plt.rcParams.update({'font.size': 16})

extracter = URLExtract()

def users(data):
    return data['Sender'].unique().tolist()

def fetch_stats(selected_user,df):
    
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    # number of msgs
    num_msgs = df.shape[0]
        # number of words
    words = []
    for message in df['Message']:
        words.extend(message.split())
        
    num_media_msgs = df[df['Message']=="<Media omitted>"].shape[0]  
    
    links = []
    for message in df['Message']:
        links.extend(extracter.find_urls(message)) 
       
    return num_msgs,len(words),num_media_msgs,len(links)   



def most_busy_user(df):
    x = df['Sender'].value_counts().head()
    percent_df = round((df['Sender'].value_counts()/df.shape[0])*100,2).reset_index().rename(columns={'count':'Percent'})
    plt.figure(figsize=(10, 8))
    sns.barplot(x = x.index,y =  x.values)
    plt.title("Most Busy Users")
    plt.xlabel('Labels')
    plt.ylabel('Values')
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded
    #return x,percent_df



def create_wordcloud(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
         
    temp =df[df['Message'] != '<Media omitted>']
    temp = temp[temp['Message'] != 'null']
    file_path = os.path.join(os.path.dirname(__file__), "stop_hinglish.txt")
    f = open(file_path,'r')
    stop_words = f.read()
    
    def remove_stopwords(message):
        y=[]
        for word in message.lower().split():
            if word not in stop_words:
                y.append(word)
        return " ".join(y)            
        
    wc = WordCloud(width=1000,height=800,min_font_size=10,background_color='white')
    temp['Message'] = temp['Message'].apply(remove_stopwords)
    df_wc = wc.generate(temp['Message'].str.cat(sep = " "))

    buffer = io.BytesIO()
    df_wc.to_image().save(buffer, format="PNG")
    img_str = base64.b64encode(buffer.getvalue()).decode()
    return img_str

def most_common_words(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    temp =df[df['Message'] != '<Media omitted>']
    file_path = os.path.join(os.path.dirname(__file__), "stop_hinglish.txt")
    f = open(file_path,'r')
    stop_words = f.read()
    words_without_stopwords = []

    for message in temp['Message']:
        for word in message.lower().split():
            if word not in stop_words:
                words_without_stopwords.append(word)    
       
    df_mostwords  = pd.DataFrame(Counter(words_without_stopwords).most_common(20))
    plt.figure(figsize=(10, 8))
    sns.barplot(x = df_mostwords[0],y =  df_mostwords[1])
    plt.title("Most Common Words")
    plt.xlabel('Labels')
    plt.ylabel('Values')
    plt.xticks(rotation = 'vertical')
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded


def emoji_helper(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    emojis = []
    for message in df['Message']:
        emojis.extend([c for c in message if emoji.is_emoji(c)])    
        
    emoji_df = pd.DataFrame(Counter(emojis).most_common(len(emojis)))

    plt.figure(figsize=(10, 8))
    plt.pie(emoji_df[1].head(), labels = emoji_df[0].head(), autopct="%0.2f")
    plt.title("Most Used Emojis")

    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded



def monthly_timeline(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    timeline  = df.groupby(['Year','Month']).count()['Message'].reset_index()
    
    time = []
    for i in range(timeline.shape[0]):
        time.append(timeline['Month'][i] + "-"+str(timeline['Year'][i]))    
        
    timeline['Time'] = time
    plt.figure(figsize=(10, 8))
    sns.lineplot(x = timeline['Time'],y =  timeline['Message'] )
    plt.title("Monthly Timeline")
    plt.xlabel('Labels')
    plt.ylabel('Values')
    plt.xticks(rotation = 'vertical')
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded



def daily_timeline(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    daily_timeline  = df.groupby(['Date']).count()['Message'].reset_index()
    plt.figure(figsize=(10, 8))
    sns.lineplot(x = daily_timeline['Date'],y =  daily_timeline['Message'])
    plt.title("Daily Timeline")
    plt.xlabel('Labels')
    plt.ylabel('Values')

    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded



def week_activity(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]

    temp = df['Day_name'].value_counts()

    plt.figure(figsize=(10, 8))
    sns.barplot(x = temp.index,y =  temp.values)
    plt.title("Daily Timeline")
    plt.xlabel('Labels')
    plt.ylabel('Values')

    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded
        


def month_activity(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]

    temp = df['Month'].value_counts()

    plt.figure(figsize=(10, 8))
    sns.barplot(y = temp.index,x =  temp.values)
    plt.title("Daily Timeline")
    plt.xlabel('Labels')
    plt.ylabel('Values')

    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded


def activity_heatmap(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    pivot_heatmap = df.pivot_table(index='Day_name',columns='Period',values='Message',aggfunc='count').fillna(0)
    plt.figure(figsize=(10, 8))
    sns.heatmap(pivot_heatmap)
    plt.title("HeatMap")
    plt.xlabel('Labels')
    plt.ylabel('Values')

    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded

    
       
    
    
        
     
    
    
    
    
        
        
    